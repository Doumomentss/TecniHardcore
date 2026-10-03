"""Audited, opt-in upgrade of the existing plaza. Never writes region files.
The server validates every target block again before executing any build command.
"""
from pathlib import Path
import json,math,sys
R=Path(__file__).resolve().parents[1]
world=Path(sys.argv[1]) if len(sys.argv)>1 else R/'server/world'
namespace={'__name__':'spawn24_audit','sys':type('Args',(),{'argv':['audit',str(world)]})()}
# Reuse the read-only Anvil decoder; pass a copied argv because its module reads it.
old=sys.argv[:];sys.argv=['audit',str(world)]
exec((R/'tools/find-sanctuary-site.py').read_text('utf8').split('coords=sorted')[0],namespace);sys.argv=old
block=namespace['block'];desired={}
def put(x,y,z,state):
 assert x*x+z*z<=32*32,(x,z)
 desired[(x,y,z)]='minecraft:'+state
# A broad but unobstructed ceremonial floor, with engraved paths and warm light.
for x in range(-12,13):
 for z in range(-12,13):
  radius=math.hypot(x,z)
  if 6<=radius<=12:
   state='deepslate_tiles' if radius<8 else 'polished_andesite' if radius<9 else 'stone_bricks' if radius<11 else 'polished_deepslate'
   if x==0 or z==0:state='chiseled_stone_bricks'
   if (x*x+z*z)%47==0:state='sea_lantern'
   put(x,95,z,state)
# Real block-letter sign, 51 blocks wide; timber and stone frame, clear central vista.
glyphs={'T':['111','010','010','010','010'],'E':['111','100','110','100','111'],'C':['111','100','100','100','111'],'N':['1001','1101','1011','1001','1001'],'I':['111','010','010','010','111'],'H':['101','101','111','101','101'],'A':['010','101','111','101','101'],'R':['110','101','110','101','101'],'D':['110','101','101','101','110'],'O':['111','101','101','101','111']}
for x in range(-26,27):
 for y in range(100,107):put(x,y,-17,'polished_blackstone_bricks')
 put(x,99,-17,'stripped_spruce_log[axis=x]');put(x,107,-17,'stripped_spruce_log[axis=x]')
 for z in [-18,-17,-16]:put(x,108,z,'spruce_slab[type=bottom]')
offset=-26
for index,char in enumerate('TECNIHARDCORE'):
 for row,line in enumerate(glyphs[char]):
  for col,on in enumerate(line):
   if on=='1':put(offset+col,105-row,-16,'waxed_cut_copper' if index%2 else 'gold_block')
 offset+=len(glyphs[char][0])+1
for x in [-24,-16,-8,8,16,24]:
 for y in range(96,100):put(x,y,-17,'stone_bricks' if y<98 else 'stripped_spruce_log[axis=y]')
 put(x,99,-16,'lantern[hanging=true]')
# Practical covered workshops on both sides, preserving existing workstations.
for side in [-1,1]:
 for z in [-6,6]:
  for x in range(26,31):
   if x*x+z*z>32*32:continue
   for y in range(96,100):put(x*side,y,z,'stone_bricks' if y==96 else 'stripped_spruce_log[axis=y]')
 for x in range(26,31):
  for z in range(-6,7):
   if x*x+z*z<=32*32:put(x*side,100,z,'spruce_slab[type=bottom]')
 for z in [-4,4]:put(27*side,99,z,'lantern[hanging=true]')
# Four compact stone/wood gateposts lead into the plaza; center stays open.
for x in [-4,4]:
 for y in range(96,101):put(x,y,26,'stone_bricks' if y<98 else 'stripped_spruce_log[axis=y]')
 put(x,101,26,'stone_brick_slab[type=bottom]');put(x,100,25,'lantern[hanging=true]')
for x in range(-4,5):put(x,101,26,'spruce_slab[type=bottom]')
for sx in [-1,1]:
 for sz in [-1,1]:
  for x,z in [(13,13),(18,11),(11,18)]:
   put(x*sx,96,z*sz,'chiseled_stone_bricks');put(x*sx,97,z*sz,'stone_brick_wall');put(x*sx,98,z*sz,'lantern')
allowed={'minecraft:'+s for s in ['air','cave_air','grass','tall_grass','fern','snow','oak_leaves','spruce_leaves','stone_bricks','mossy_stone_bricks','cracked_stone_bricks','chiseled_stone_bricks','polished_andesite','stone','stone_brick_wall','stone_brick_slab','spruce_slab','spruce_stairs','spruce_fence','stripped_spruce_log','lantern','deepslate_tiles','polished_deepslate','gravel','grass_block','dirt','coarse_dirt','sea_lantern','polished_blackstone_bricks','waxed_cut_copper','gold_block']}
conflicts=[];checks=[];commands=[]
for pos,state in sorted(desired.items()):
 oldState=block(*pos)
 if oldState=='minecraft:gilded_blackstone':continue
 if oldState not in allowed:conflicts.append({'position':pos,'block':oldState});continue
 x,y,z=pos;checks.append(f'execute unless block {x} {y} {z} {oldState} run scoreboard players set #valid th_spawn24 0')
 commands.append(f'setblock {x} {y} {z} {state}')
if conflicts:
 report=R/'tools/test-runtime/spawn24-conflicts.json';report.write_text(json.dumps(conflicts,indent=2),encoding='utf8');raise RuntimeError(f'Occupied blocks: {len(conflicts)}; preserved; see {report}')
out=R/'tools/spawn24-pack'
def write(relative,data):p=out/relative;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data,encoding='utf8')
write('pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':'TecniHardcore: mejora auditada de plaza 2.4 (manual)'}}))
write('data/tecni_spawn24/functions/upgrade.mcfunction','scoreboard objectives add th_spawn24 dummy\nexecute unless score #built th_spawn24 matches 1.. run function tecni_spawn24:validate\n')
write('data/tecni_spawn24/functions/validate.mcfunction','scoreboard players set #valid th_spawn24 1\n'+'\n'.join(checks)+'\nexecute if score #valid th_spawn24 matches 1 run function tecni_spawn24:build\nexecute unless score #valid th_spawn24 matches 1 run say Mejora del spawn cancelada: hay bloques nuevos. No se ha cambiado nada.\n')
write('data/tecni_spawn24/functions/build.mcfunction','\n'.join(commands)+'\nscoreboard players set #built th_spawn24 1\nsay Plaza TecniHardcore mejorada: cartel, talleres y espacio ceremonial.\n')
report={'world':str(world),'targetBlocks':len(desired),'runtimeValidation':True,'sign':'TECNIHARDCORE','signBounds':[-25,101,-16,25,105,-16],'coreUntouched':True,'conflicts':conflicts}
(R/'tools/test-runtime/spawn24-plan.json').write_text(json.dumps(report,indent=2),encoding='utf8');print(json.dumps(report))
