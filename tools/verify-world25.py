"""Read saved Anvil chunks: preservation at spawn and actual new biome palettes."""
from pathlib import Path
import io,zlib,hashlib,json,nbtlib,collections,numpy as np,sys
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'tools/test-runtime/expansion25'
WORLD=Path(sys.argv[1]) if len(sys.argv)>1 else BASE/'server/world';OLD=Path(json.loads((BASE/'backup-restoration-result.json').read_text())['restoration'])
def chunks(directory):
 for p in directory.glob('r.*.*.mca'):
  buf=p.read_bytes()
  for i in range(1024):
   off=int.from_bytes(buf[i*4:i*4+3],'big')*4096
   if not off:continue
   length=int.from_bytes(buf[off:off+4],'big');kind=buf[off+4];raw=buf[off+5:off+4+length]
   if kind!=2:continue
   n=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw)))
   yield n
def signature(n):
 selected=nbtlib.Compound({'sections':nbtlib.List[nbtlib.Compound]([nbtlib.Compound({k:s[k] for k in ('Y','block_states','biomes') if k in s}) for s in n.get('sections',[])])})
 out=io.BytesIO();nbtlib.File(selected).write(out);return hashlib.sha256(out.getvalue()).hexdigest()
old={(int(n['xPos']),int(n['zPos'])):n for n in chunks(OLD/'region') if abs(int(n['xPos']))<=3 and abs(int(n['zPos']))<=3}
new={(int(n['xPos']),int(n['zPos'])):n for n in chunks(WORLD/'region') if abs(int(n['xPos']))<=3 and abs(int(n['zPos']))<=3}
assert old,'Original spawn chunks required'
def decoded(s):
 states=s.get('block_states',{});palette=states.get('palette',[nbtlib.Compound({'Name':nbtlib.String('minecraft:air')})]);names=np.array([str(p['Name'])+json.dumps(p.get('Properties',{}).unpack() if 'Properties' in p else {},sort_keys=True) for p in palette])
 if len(names)==1:return np.repeat(names[0],4096)
 bits=max(4,(len(names)-1).bit_length());per=64//bits;raw=np.asarray(states['data'],dtype=np.int64).view(np.uint64);idx=np.arange(4096);ids=(raw[idx//per]>>((idx%per)*bits).astype(np.uint64))&np.uint64((1<<bits)-1);return names[ids.astype(int)]
differences=collections.Counter();positions=[];changed=[]
for pos,n in old.items():
 sections={int(s['Y']):s for s in new[pos]['sections']};different=False
 for s in n['sections']:
  y=int(s['Y']);a=decoded(s);b=decoded(sections.get(y,{}));indices=np.flatnonzero(a!=b)
  for index in indices:
   different=True;differences[(str(a[index]),str(b[index]))]+=1
   if len(positions)<30:positions.append({'x':pos[0]*16+int(index)%16,'y':y*16+int(index)//256,'z':pos[1]*16+(int(index)//16)%16,'old':str(a[index]),'new':str(b[index])})
 if different:changed.append(pos)
biomes=set();structures=set();generated=0
for directory in (WORLD/'region',WORLD/'DIM-1/region'):
 for n in chunks(directory):
  if abs(int(n['xPos']))<4000 and abs(int(n['zPos']))<4000:continue
  generated+=1
  for s in n.get('sections',[]):biomes.update(str(b) for b in s.get('biomes',{}).get('palette',[]))
  structures.update(str(key) for key,value in n.get('structures',{}).get('starts',{}).items() if str(value.get('id',''))!='INVALID')
result={'spawnChunksCompared':len(old),'changedSpawnChunks':changed,'blockChanges':[{'old':a,'new':b,'count':count} for (a,b),count in differences.items()],'sampleChanges':positions,'generatedChunksExamined':generated,'biomes':sorted(biomes),'structures':sorted(structures),'bothBiomePacksGenerated':any(b.startswith('natures_spirit:') for b in biomes) and any(b.startswith('regions_unexplored:') for b in biomes)}
(BASE/('fresh-world-preservation-results.json' if len(sys.argv)>1 else 'world-preservation-results.json')).write_text(json.dumps(result,indent=2))
print(json.dumps(result,indent=2));assert not changed,'Existing spawn terrain changed';assert result['bothBiomePacksGenerated'],'Both biome packs must actually generate'
