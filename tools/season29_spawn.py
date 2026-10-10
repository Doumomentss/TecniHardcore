"""Read-only Anvil export of the real 2.8.1 plaza, plus one-shot 2.9 import functions.

Usage: python tools/season29_spawn.py OLD_WORLD NEW_WORLD OUTPUT_PACK
Run the generated tecni_season29:prepare manually on an isolated server. Never
load/tick automatically. It refuses unknown columns or a wet/steep spawn.
"""
import io, json, math, sys, zlib
from pathlib import Path
import nbtlib

old_root, new_root, out = map(Path, sys.argv[1:4])
FLOOR, INNER, OUTER = 95, 32, 56

class Anvil:
    def __init__(self, root):
        self.root, self.regions, self.chunks = root, {}, {}
    def chunk(self, cx, cz):
        key = cx, cz
        if key in self.chunks: return self.chunks[key]
        rx, rz = cx // 32, cz // 32
        region = self.regions.get((rx, rz))
        if region is None:
            path = self.root / 'region' / f'r.{rx}.{rz}.mca'
            region = path.read_bytes() if path.exists() else b''
            self.regions[rx, rz] = region
        at = ((cx % 32) + (cz % 32) * 32) * 4
        offset = int.from_bytes(region[at:at+3], 'big') * 4096
        if not offset: self.chunks[key] = None; return None
        length = int.from_bytes(region[offset:offset+4], 'big')
        if region[offset+4] != 2: raise ValueError('Unsupported region compression')
        tag = nbtlib.File.parse(io.BytesIO(zlib.decompress(region[offset+5:offset+4+length])))
        sections = {int(s['Y']): s for s in tag.get('sections', [])}
        self.chunks[key] = tag, sections
        return self.chunks[key]
    def state(self, x, y, z):
        chunk = self.chunk(x//16, z//16)
        if chunk is None: raise ValueError(f'Chunk not generated: {x//16}, {z//16}')
        section = chunk[1].get(y//16)
        if section is None or 'block_states' not in section: return ('minecraft:air', {})
        states = section['block_states']; palette = states['palette']
        index = (y%16)*256+(z%16)*16+x%16
        if len(palette) == 1: selected = palette[0]
        else:
            bits = max(4, (len(palette)-1).bit_length()); per = 64//bits
            value = (int(states['data'][index//per]) & ((1<<64)-1)) >> ((index%per)*bits) & ((1<<bits)-1)
            selected = palette[value]
        return str(selected['Name']), {str(k):str(v) for k,v in selected.get('Properties',{}).items()}
    def top(self, x, z):
        chunk = self.chunk(x//16,z//16)
        if chunk is None: raise ValueError(f'Chunk not generated: {x//16}, {z//16}')
        data = chunk[0]['Heightmaps']['MOTION_BLOCKING_NO_LEAVES']
        i = (z%16)*16+x%16
        return ((int(data[i//7]) & ((1<<64)-1)) >> ((i%7)*9) & 511)-64

old, new = Anvil(old_root), Anvil(new_root)
columns = [(x,z) for x in range(-OUTER,OUTER+1) for z in range(-OUTER,OUTER+1) if math.hypot(x,z)<=OUTER]
sample = [(x,z,new.top(x,z),new.state(x,new.top(x,z)-1,z)[0]) for x,z in columns]
water = [s for s in sample if s[3] in ('minecraft:water','minecraft:lava','minecraft:ice')]
heights = [s[2] for s in sample]
if water or min(heights)<50 or max(heights)>120 or max(heights)-min(heights)>65:
    raise SystemExit(f'Unsafe spawn: heights {min(heights)}..{max(heights)}, wet columns {len(water)}')

def cmd_state(name, props):
    return name + ('['+','.join(f'{k}={v}' for k,v in sorted(props.items()))+']' if props else '')
commands = []
def emit(s): commands.append(s)
emit('forceload add -64 -64 63 63')
# A natural dirt embankment blends the fixed plaza floor into the new terrain.
for x,z,h,_ in sample:
    r = math.hypot(x,z)
    blend = max(0,min(1,(r-INNER)/(OUTER-INNER)))
    blend = blend*blend*(3-2*blend)
    target = FLOOR if r<=INNER else round(FLOOR*(1-blend)+(h-1)*blend)
    bottom = min(h-1,target)-1
    if target>bottom:
        emit(f'fill {x} {bottom} {z} {x} {target-1} {z} minecraft:dirt')
    emit(f'setblock {x} {target} {z} minecraft:grass_block')
    upper = max(145,h+8) if r<=INNER else max(h+3,target+3)
    if target+1<=upper:emit(f'fill {x} {target+1} {z} {x} {upper} {z} minecraft:air')

snapshot=[]
for x,z in columns:
    if math.hypot(x,z)>INNER:continue
    for y in range(FLOOR,131):
        name,props=old.state(x,y,z)
        if name in ('minecraft:air','minecraft:cave_air','minecraft:void_air'):continue
        snapshot.append([x,y,z,name,props])
        emit(f'setblock {x} {y} {z} {cmd_state(name,props)}')
emit('setworldspawn 0 96 20 180')
emit('gamerule spawnRadius 0')
emit('forceload remove -64 -64 63 63')
emit('say Plaza real de TecniHardcore importada en el mundo nuevo.')

def write(relative, content):
    path=out/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content,encoding='utf-8')
write('pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':'One-shot import, TecniHardcore season 2.9'}},ensure_ascii=False))
write('data/tecni_season29/functions/prepare.mcfunction','scoreboard objectives add th_s29 dummy\nexecute unless score #built th_s29 matches 1.. run function tecni_season29:phase_0\n')
for i,offset in enumerate(range(0,len(commands),500)):
    part=commands[offset:offset+500]
    if offset+500<len(commands):part.append(f'schedule function tecni_season29:phase_{i+1} 2t replace')
    else:part.append('scoreboard players set #built th_s29 1')
    write(f'data/tecni_season29/functions/phase_{i}.mcfunction','\n'.join(part)+'\n')
write('snapshot.json',json.dumps(snapshot,separators=(',',':')))
print(json.dumps({'blocks':len(snapshot),'commands':len(commands),'phases':math.ceil(len(commands)/500),'heightRange':[min(heights),max(heights)],'wet':len(water),'output':str(out)}))
