"""Read-only saved-world verification; flowing fluids are not construction."""
from pathlib import Path
from collections import Counter
import io, json, zlib, zipfile, gzip
import nbtlib, numpy as np

R = Path(__file__).resolve().parents[1]
out = R/'tools/test-runtime/tornado262'
jar = R/'server/mods/DungeonsArise-1.20.1-2.1.57-fabric-release.jar'
with zipfile.ZipFile(jar) as z:
    template = next(n for n in z.namelist() if n.endswith('/keep_kayra_main_0.nbt'))
    data = nbtlib.File.parse(io.BytesIO(gzip.decompress(z.read(template))))
    names = [str(v['Name']) for v in data['palette']]
    original = Counter(names[int(b['state'])] for b in data['blocks'])
    del data

remaining = Counter()
region = R/'tools/test-runtime/expansion25/server/world/dimensions/tecnihardcore/eventos/region'
for file in region.glob('r.68.*.mca'):
    with file.open('rb') as f:
        locations = f.read(4096)
        for i in range(1024):
            offset = int.from_bytes(locations[i*4:i*4+3], 'big')
            if not offset: continue
            f.seek(offset*4096)
            length = int.from_bytes(f.read(4), 'big')
            compression = f.read(1)[0]
            raw = f.read(length-1)
            assert compression == 2
            chunk = nbtlib.File.parse(io.BytesIO(zlib.decompress(raw)))
            cx, cz = int(chunk['xPos'])*16, int(chunk['zPos'])*16
            if cx>35071 or cx+15<34929 or cz>71 or cz+15<-71: continue
            for section in chunk['sections']:
                cy = int(section['Y'])*16
                if cy>275 or cy+15<16 or 'block_states' not in section: continue
                states = section['block_states']
                palette = [str(v['Name']) for v in states['palette']]
                if len(palette)==1: ids=np.zeros(4096,dtype=np.uint64)
                else:
                    bits=max(4,(len(palette)-1).bit_length()); per=64//bits
                    packed=np.asarray(states['data'],dtype=np.int64).view(np.uint64)
                    indices=np.arange(4096)
                    ids=(packed[indices//per]>>((indices%per)*bits).astype(np.uint64))&((1<<bits)-1)
                box=ids.reshape(16,16,16)[max(0,16-cy):min(16,276-cy),max(0,-71-cz):min(16,72-cz),max(0,34929-cx):min(16,35072-cx)]
                unique, counts=np.unique(box,return_counts=True)
                for k,v in zip(unique,counts): remaining[palette[int(k)]] += int(v)

ignored={'minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void','minecraft:water','minecraft:lava'}
baseline=sum(v for k,v in original.items() if k not in ignored)
solids=sum(v for k,v in remaining.items() if k not in ignored)
assert sum(remaining.values())==143*260*143
assert solids<baseline*.2
result={'method':'Original structure NBT versus final saved Anvil; excludes flowing water/lava and air',
        'activeSeconds':540,'originalTemplateConstruction':baseline,'remainingConstruction':solids,
        'removedFraction':1-solids/baseline,'remainingBlocks':dict(remaining.most_common()),
        'note':'Initial live counter incorrectly counted flowing fluids as construction. Its failed result remains preserved. Original template and initial placed volume differed by 27 non-air positions.',
        'ticks':[json.loads((R/f'tools/test-runtime/expansion25/server/qa-results/tornado262-{i}-ticks.json').read_text()) for i in (1,2,3)]}
(out/'saved-world-proof.json').write_text(json.dumps(result,indent=2),encoding='utf8')
print(f'PASS saved Keep Kayra: {baseline:,} original construction; {solids:,} remaining; {result["removedFraction"]:.3%} removed. Fluids reported separately.')
