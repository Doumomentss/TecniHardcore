"""Static audit of the clean season baseline; does not mutate either world."""
import hashlib, io, json, math, sys, zipfile, zlib
from pathlib import Path
import nbtlib

root=Path(__file__).resolve().parents[1]
old=root/'server/world'
new=root/'tools/test-runtime/season29-validated-baseline'
archive=root/'season-archives/season-2026-10-09-pre29.zip'
manifest=json.loads((root/'season-archives/season-2026-10-09-pre29.sha256.json').read_text())
assert archive.exists() and hashlib.file_digest(archive.open('rb'),'sha256').hexdigest()==manifest['sha256']
assert (root/'season-archives/season-2026-10-09-pre29.restore-ok').exists()

class Reader:
    def __init__(self,path):self.path,self.regions,self.chunks=path,{},{}
    def block(self,x,y,z):
        cx,cz=x//16,z//16;key=cx,cz
        if key not in self.chunks:
            rx,rz=cx//32,cz//32
            if (rx,rz) not in self.regions:self.regions[rx,rz]=(self.path/'region'/f'r.{rx}.{rz}.mca').read_bytes()
            data=self.regions[rx,rz];at=((cx%32)+(cz%32)*32)*4;off=int.from_bytes(data[at:at+3],'big')*4096
            if not off:raise AssertionError(f'Missing chunk {key}')
            length=int.from_bytes(data[off:off+4],'big');assert data[off+4]==2
            n=nbtlib.File.parse(io.BytesIO(zlib.decompress(data[off+5:off+4+length])))
            self.chunks[key]={int(s['Y']):s for s in n.get('sections',[])}
        s=self.chunks[key].get(y//16)
        if not s or 'block_states' not in s:return ('minecraft:air',{})
        states=s['block_states'];palette=states['palette'];index=(y%16)*256+(z%16)*16+x%16
        if len(palette)==1:value=0
        else:
            bits=max(4,(len(palette)-1).bit_length());per=64//bits
            value=(int(states['data'][index//per])&((1<<64)-1))>>((index%per)*bits)&((1<<bits)-1)
        selected=palette[value]
        return str(selected['Name']),{str(k):str(v) for k,v in selected.get('Properties',{}).items()}

a,b=Reader(old),Reader(new)
total=0
for x in range(-32,33):
    for z in range(-32,33):
        if math.hypot(x,z)>32:continue
        for y in range(95,131):
            assert a.block(x,y,z)==b.block(x,y,z),(x,y,z,a.block(x,y,z),b.block(x,y,z))
            total+=1
assert b.block(0,96,0)[0]=='tecnihardcore:santuario'
assert b.block(6,96,-17)[0]=='tecnihardcore:tablon_reliquias'
assert b.block(0,95,20)[0]!='minecraft:air' and b.block(0,96,20)[0]=='minecraft:air'
old_data=nbtlib.load(old/'level.dat')['Data'];new_data=nbtlib.load(new/'level.dat')['Data']
assert int(new_data['WorldGenSettings']['seed'])==20261009!=int(old_data['WorldGenSettings']['seed'])
assert [int(new_data[n]) for n in ('SpawnX','SpawnY','SpawnZ')]==[0,96,20]
assert not any(any((new/sub).glob('*.dat')) for sub in ['playerdata','advancements','stats'])
assert not (new/'datapacks/hardcore_3_vidas').exists()
assert not (new/'datapacks/tecni_spawn24').exists()
assert not (new/'datapacks/tecni_season29').exists()
assert (new/'datapacks/tecni_spawn/data/tecnihardcore/worldgen/placed_feature/sanctuary.json').exists()
souls=json.loads((new/'tecnihardcore-souls.json').read_text())
assert souls['maxLives']==5 and souls['players']=={} and souls['sanctuaries']==['minecraft:overworld|0|96|0']
social=json.loads((new/'tecnihardcore-social.json').read_text())['data']
assert all(not social[k] for k in ('teams','truces','wallets','offers','parcels','quests'))
expansion=json.loads((new/'tecnihardcore-expansion.json').read_text())
assert not expansion['mounts'] and not expansion['rewards'] and not expansion['defeated']
intro=json.loads((new/'tecnihardcore-intro.json').read_text())
assert intro['schema']==1 and not intro['completed']
config=json.loads((root/'server/config/tecnihardcore/npcs.json').read_text())
for npc in config.values():
    skin=new/'tecni-npc-skins'/f"{npc['skinHash']}.png"
    assert skin.exists() and hashlib.sha256(skin.read_bytes()).hexdigest()==npc['skinHash']
assert any((old/'region').glob('*.mca')) and any((new/'region').glob('*.mca'))
print(json.dumps({'seed':int(new_data['WorldGenSettings']['seed']),'spawn':[0,96,20],'matchingPlazaBlocks':total,'npcSkins':len(config),'oldArchiveSha256':manifest['sha256'],'oldArchiveFiles':len(manifest['files']),'baseline':str(new)}))
