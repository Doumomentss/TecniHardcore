"""Create the spawn datapack from an audited world; apply through Minecraft commands.

Nothing writes region NBT. The construction function is manual, never a load/tick
function, and its scoreboard guard prevents accidental reapplication.
"""
import collections,json,math,sys
from pathlib import Path
source=Path(__file__).with_name('find-sanctuary-site.py').read_text()
exec(source.split('coords=sorted(')[0])
OUT=Path('tools/spawn-pack');FLOOR=95;CORE=FLOOR+1;RADIUS=32;EDGE=56
commands=[];changed=collections.Counter();conflicts=[];maxheight=CORE+18
def cmd(s):commands.append(s)
def setblock(x,y,z,block):cmd(f'setblock {x} {y} {z} minecraft:{block}')
def fill(x,z,y1,y2,block):
    if y1<=y2:cmd(f'fill {x} {y1} {z} {x} {y2} {z} minecraft:{block}')
air={'minecraft:air','minecraft:cave_air','minecraft:void_air'}
vegetation={'minecraft:'+b for b in ['oak_log','birch_log','oak_leaves','birch_leaves','grass','tall_grass','fern','large_fern','snow','dandelion','poppy','azure_bluet','oxeye_daisy','cornflower','lily_of_the_valley','sunflower','lilac','rose_bush','peony']}
groundtypes=natural|{'minecraft:'+b for b in ['diorite','andesite','granite','coal_ore','iron_ore','copper_ore','emerald_ore','gold_ore','redstone_ore','lapis_ore','diamond_ore','infested_stone','gravel','sand','clay','water','lava','calcite','tuff','snow']}
oldruin={'minecraft:'+b for b in ['polished_blackstone_bricks','deepslate_bricks','chiseled_polished_blackstone','deepslate_tiles','gilded_blackstone','soul_lantern','polished_blackstone_slab']}|{'tecnihardcore:santuario'}
for x in range(-EDGE,EDGE+1):
    for z in range(-EDGE,EDGE+1):
        r=math.hypot(x,z)
        if r>EDGE:continue
        surface=top(x,z)
        if surface is None:raise SystemExit(f'Unexplored column {x} {z}; refusing blind edits.')
        terrain=surface-1
        while terrain>50 and block(x,terrain,z) in air|vegetation:terrain-=1
        blend=max(0,min(1,(r-RADIUS)/(EDGE-RADIUS)))
        blend=blend*blend*(3-2*blend)
        target=FLOOR if r<=RADIUS else round(FLOOR*(1-blend)+terrain*blend)
        upper=max(surface+5,160 if r<=RADIUS else surface+5)
        lower=min(terrain,target)-1
        for y in range(lower,upper+1):
            b=block(x,y,z)
            if b not in air|vegetation|groundtypes:
                old=(-27<=x<=-17 and 33<=z<=43 and 84<=y<=93 and b in oldruin)
                relocate=(x,y,z)==(-16,102,12) and b=='minecraft:crafting_table'
                if not old and not relocate:conflicts.append([x,y,z,b])
        # A single shared plaza height; outside it, a smooth earth embankment.
        if r<=RADIUS or target!=terrain:
            fill(x,z,min(terrain,target)-1,target-1,'dirt')
            setblock(x,target,z,'grass_block')
            fill(x,z,target+1,upper,'air')
        elif -27<=x<=-17 and 33<=z<=43:
            fill(x,z,target+1,max(upper,94),'air')
            setblock(x,target,z,'grass_block')
        if r>RADIUS:continue
        a,b=abs(x),abs(z)
        if r<8:material='deepslate_tiles' if (x*13+z*7)%11 else 'polished_deepslate'
        elif r<9:material='polished_blackstone_bricks'
        elif r<10:material='polished_andesite'
        elif r<14:material='stone_bricks' if (x+z)%7 else 'cracked_stone_bricks'
        elif r<15:material='polished_andesite'
        elif a<=3 or b<=3:material='stone_bricks' if (a+b)%7 else 'andesite'
        elif r>=30:material='stone_bricks' if (a+b)%5 else 'cobblestone'
        elif 19<r<22:material='gravel' if (x*11+z*17)%4 else 'andesite'
        else:material='grass_block'
        setblock(x,FLOOR,z,material);changed[material]+=1
if conflicts:raise SystemExit('Construction conflicts; no pack written: '+json.dumps(conflicts[:30]))
# Relocate the old core, rather than leaving a second usable sanctuary.
cmd('setblock -22 88 38 minecraft:dirt')
for x in [-6,6]:
    for z in [-6,6]:setblock(x,FLOOR,z,'gilded_blackstone')
for x,z in [(0,8),(0,-8),(8,0),(-8,0),(10,10),(-10,10),(10,-10),(-10,-10)]:
    setblock(x,FLOOR,z,'sea_lantern');setblock(x,CORE,z,'gray_carpet')
# Quiet perimeter lighting, low benches and trimmed hedges leave the centre open.
for x,z in [(24,10),(24,-10),(-24,10),(-24,-10),(10,24),(-10,24),(10,-24),(-10,-24)]:
    setblock(x,CORE,z,'stone_brick_wall');setblock(x,CORE+1,z,'spruce_fence');setblock(x,CORE+2,z,'lantern')
for sign in [-1,1]:
    for t in range(-2,3):
        setblock(t,CORE,sign*25,'spruce_stairs[facing='+('north' if sign>0 else 'south')+']')
        setblock(sign*25,CORE,t,'spruce_stairs[facing='+('west' if sign>0 else 'east')+']')
    for t in [-3,3]:
        setblock(t,CORE,sign*25,'stone_brick_wall');setblock(sign*25,CORE,t,'stone_brick_wall')
for sx2 in [-1,1]:
    for sz2 in [-1,1]:
        for x in range(15,19):
            for z in range(15,19):
                if x in [15,18] or z in [15,18]:setblock(x*sx2,CORE,z*sz2,'oak_leaves[persistent=true]')
        for x,z in [(16,16),(17,17)]:setblock(x*sx2,CORE,z*sz2,'fern')
# Small, practical utility alcoves, away from every cinematic sightline.
for x in [-28,28]:
    for y in [CORE,CORE+1]:setblock(x,y,-5,'stripped_spruce_log[axis=y]');setblock(x,y,5,'stripped_spruce_log[axis=y]')
    for z in range(-5,6):setblock(x,CORE+2,z,'spruce_slab[type=bottom]')
setblock(-27,CORE,-3,'crafting_table')
setblock(-27,CORE,-2,'stonecutter[facing=east]')
# Use the mod's administrative placement so its persistent registry is updated.
cmd(f'fill -5 {FLOOR} -5 5 {FLOOR} 5 minecraft:stone')
cmd(f'tecni santuario crear 0 {CORE} 0')
# Remove the little ruin's pillars while retaining the registered central core.
for x1,z1,x2,z2 in [(-5,-5,-1,5),(1,-5,5,5),(0,-5,0,-1),(0,1,0,5)]:
    cmd(f'fill {x1} {CORE} {z1} {x2} {CORE+13} {z2} minecraft:air')
cmd(f'fill 0 {CORE+1} 0 0 {CORE+13} 0 minecraft:air')
for x in range(-5,6):
    for z in range(-5,6):setblock(x,FLOOR,z,'deepslate_tiles' if (x*13+z*7)%11 else 'polished_deepslate')
cmd(f'setworldspawn 0 {CORE} 0 180')
cmd('gamerule spawnRadius 18')
cmd('scoreboard players set #built th_spawn 1')
cmd('forceload remove -64 -64 63 63')
cmd('say Spawn construido: plaza en 0, 0; santuario central unico.')
OUT.mkdir(parents=True,exist_ok=True)
def write(relative,text):
    p=OUT/relative;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
write('pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':'TecniHardcore: spawn central y santuario unico (servidor)'}},ensure_ascii=False))
write('data/tecnihardcore/worldgen/placed_feature/sanctuary.json',json.dumps({'feature':'tecnihardcore:sanctuary','placement':[{'type':'minecraft:count','count':0}]}))
write('data/tecni_spawn/functions/build.mcfunction','scoreboard objectives add th_spawn dummy\nexecute unless score #built th_spawn matches 1.. run function tecni_spawn:prepare\n')
write('data/tecni_spawn/functions/prepare.mcfunction','scoreboard players set #built th_spawn 2\nforceload add -64 -64 63 63\nschedule function tecni_spawn:phase_0 2s replace\n')
# Bound command work per tick; also keeps terminal output manageable.
for i,offset in enumerate(range(0,len(commands),700)):
    part=commands[offset:offset+700]
    if offset+700<len(commands):part.append(f'schedule function tecni_spawn:phase_{i+1} 10t replace')
    write(f'data/tecni_spawn/functions/phase_{i}.mcfunction','\n'.join(part)+'\n')
report={'floor':FLOOR,'core':[0,CORE,0],'clearRadius':RADIUS,'blendRadius':EDGE,'commands':len(commands),'phases':math.ceil(len(commands)/700),'oldCore':[-22,88,38],'conflicts':conflicts,'floorMaterials':dict(changed),'generationDisabled':True,'idempotent':True}
Path('tools/test-runtime/spawn-plan.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report))
