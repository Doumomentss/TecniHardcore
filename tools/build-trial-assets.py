"""Editable original stone guardian, atlas, animations and positional audio."""
from pathlib import Path
import json,random,math,wave,subprocess
import numpy as np
from PIL import Image
import imageio_ffmpeg
A=Path(__file__).resolve().parents[1]/'custom_mods/hardcore/src/main/resources/assets/tecnihardcore'
def js(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2),'utf-8')
def cube(o,s,m='stone',**kw):return dict(origin=o,size=s,uv={'stone':[0,0],'gold':[128,0],'blue':[0,128],'glow':[128,128]}[m],**kw)
b=[dict(name='body',pivot=[0,32,0],cubes=[cube([-12,23,-7],[24,20,14]),cube([-10,19,-6],[20,5,12],'gold'),cube([-10,27,-8],[20,12,2],'gold'),cube([-6,28,-10],[12,10,3],'blue'),cube([-3,30,-11],[6,6,2],'glow',pivot=[0,33,-10],rotation=[0,0,45])]),dict(name='head',parent='body',pivot=[0,43,0],cubes=[cube([-7,43,-6],[14,12,12]),cube([-8,51,-7],[16,2,14],'gold'),cube([-5,48,-7],[10,2,1],'glow'),cube([-1,44,-7],[2,3,1],'gold'),cube([-4,55,-2],[8,5,4],'blue',pivot=[0,57,0],rotation=[0,0,45])])]
for sign,name in [(-1,'left'),(1,'right')]:
 x=sign*17
 b.append(dict(name=f'{name}_arm',parent='body',pivot=[x,40,0],cubes=[cube([x-6,32,-7],[12,12,14]),cube([x-7,40,-8],[14,4,16],'gold'),cube([x-4,19,-4],[8,14,8]),cube([x-5,17,-5],[10,3,10],'gold'),cube([x-6,10,-6],[12,8,12]),cube([x-4,12,-7],[8,4,1],'glow')]))
 x=sign*7
 b.append(dict(name=f'{name}_leg',pivot=[x,23,0],cubes=[cube([x-5,8,-5],[10,15,10]),cube([x-6,8,-6],[12,4,12],'gold'),cube([x-6,0,-9],[12,8,16]),cube([x-4,4,-10],[8,2,1],'glow')]))
for i in range(4):
 a=i*math.pi/2;x,z=math.cos(a)*14,math.sin(a)*14
 b.append(dict(name=f'orbit_{i}',parent='body',pivot=[0,33,0],cubes=[cube([x-2,32,z-2],[4,8,4],'blue',pivot=[x,36,z],rotation=[0,45,25]),cube([x-1,34,z-1],[2,4,2],'glow')]))
def geo(name,bones,bounds):js(A/f'geo/{name}.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+name,'texture_width':256,'texture_height':256,'visible_bounds_width':bounds,'visible_bounds_height':5,'visible_bounds_offset':[0,2,0]},'bones':bones}]})
geo('custodio',b,6);geo('trial_shard',[dict(name='shard',pivot=[0,0,0],cubes=[cube([-2,-5,-2],[4,10,4],'blue',pivot=[0,0,0],rotation=[0,45,30]),cube([-1,-3,-1],[2,6,2],'glow')])],2)
animations={}
def anim(name,length,bones):animations['animation.custodio.'+name]={'loop':True,'animation_length':length,'bones':bones}
orbit={f'orbit_{i}':{'rotation':{'0':[0,i*90,0],'4':[0,i*90+360,0]}} for i in range(4)}
anim('idle',4,dict(orbit,body={'position':{'0':[0,0,0],'2':[0,.6,0],'4':[0,0,0]}}))
anim('walk',1,{'left_leg':{'rotation':{'0':[20,0,0],'.5':[-20,0,0],'1':[20,0,0]}},'right_leg':{'rotation':{'0':[-20,0,0],'.5':[20,0,0],'1':[-20,0,0]}},'left_arm':{'rotation':{'0':[-12,0,0],'.5':[12,0,0],'1':[-12,0,0]}},'right_arm':{'rotation':{'0':[12,0,0],'.5':[-12,0,0],'1':[12,0,0]}}})
anim('slam',3.5,{'left_arm':{'rotation':{'0':[0,0,0],'1':[-145,0,-15],'1.7':[-145,0,-15],'1.9':[20,0,0],'3.5':[0,0,0]}},'right_arm':{'rotation':{'0':[0,0,0],'1':[-145,0,15],'1.7':[-145,0,15],'1.9':[20,0,0],'3.5':[0,0,0]}},'body':{'rotation':{'0':[0,0,0],'1.7':[-10,0,0],'1.9':[15,0,0],'3.5':[0,0,0]}}})
anim('cast',3.5,{'left_arm':{'rotation':{'0':[0,0,0],'.8':[-80,0,-30],'2.5':[-80,0,-30],'3.5':[0,0,0]}},'right_arm':{'rotation':{'0':[0,0,0],'.8':[-80,0,30],'2.5':[-80,0,30],'3.5':[0,0,0]}},**orbit})
animations['animation.shard.spin']={'loop':True,'animation_length':1,'bones':{'shard':{'rotation':{'0':[0,0,0],'1':[0,360,180]}}}}
js(A/'animations/custodio.animation.json',{'format_version':'1.8.0','animations':animations})
r=random.Random(230);atlas=Image.new('RGBA',(256,256));glow=Image.new('RGBA',atlas.size)
for ox,oy,base in [(0,0,(47,58,64)),(128,0,(156,113,45)),(0,128,(28,145,178)),(128,128,(109,231,255))]:
 for y in range(128):
  for x in range(128):
   n=r.randrange(-12,13);edge=-15 if x%16==0 or y%16==0 else 0;facet=20 if oy and (x+y)%24<5 else 0
   color=tuple(max(0,min(255,c+n+edge+facet)) for c in base)+(255,);atlas.putpixel((ox+x,oy+y),color)
   if ox==128 and oy==128:glow.putpixel((ox+x,oy+y),color)
(A/'textures/entity').mkdir(parents=True,exist_ok=True);atlas.save(A/'textures/entity/custodio.png');glow.save(A/'textures/entity/custodio_glowmask.png')
sounds=json.loads((A/'sounds.json').read_text('utf-8'))
for name,freq,duration in [('wake',110,1.6),('strike',60,.8)]:
 rate=44100;t=np.arange(int(rate*duration))/rate;signal=(np.sin(2*np.pi*(freq*t+12*t*t))*.4+np.random.default_rng(23).normal(0,.07,len(t)))*np.exp(-t*3)*np.minimum(1,t/.03)
 out=A/'sounds/boss';out.mkdir(parents=True,exist_ok=True);wav=out/f'{name}.wav'
 with wave.open(str(wav),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes((signal*32767).astype('<i2').tobytes())
 subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(),'-v','error','-y','-i',str(wav),str(out/f'{name}.ogg')],check=True);wav.unlink();sounds['boss.'+name]={'sounds':['tecnihardcore:boss/'+name],'subtitle':'subtitles.tecnihardcore.boss_'+name}
js(A/'sounds.json',sounds)
# The poster uses real registered item renderers; geometry remains vanilla and editable.
elements=[]
def box(f,t,texture):elements.append({'from':f,'to':t,'faces':{side:{'texture':'#'+texture,'uv':[0,0,16,16]} for side in ['north','south','east','west','up','down']}})
box([-16,10,6],[32,32,10],'board');box([-16,10,5],[32,12,11],'frame');box([-16,30,5],[32,32,11],'frame');box([-16,10,5],[-14,32,11],'frame');box([30,10,5],[32,32,11],'frame');box([-12,0,7],[-9,10,10],'frame');box([25,0,7],[28,10,10],'frame')
js(A/'models/block/tablon_reliquias.json',{'textures':{'board':'minecraft:block/dark_oak_planks','frame':'minecraft:block/stripped_spruce_log','particle':'minecraft:block/dark_oak_planks'},'elements':elements})
js(A/'blockstates/tablon_reliquias.json',{'variants':{'':{'model':'tecnihardcore:block/tablon_reliquias'}}})
for p in (A/'lang').glob('*.json'):
 data=json.loads(p.read_text('utf-8'));data.update({'entity.tecnihardcore.custodio_pizarra':'Custodio de Pizarra','block.tecnihardcore.tablon_reliquias':'Tablón de reliquias','block.tecnihardcore.tablon_panel':'Tablón de reliquias','subtitles.tecnihardcore.boss_wake':'El custodio prepara un ataque','subtitles.tecnihardcore.boss_strike':'El cristal golpea'});js(p,data)
print(f'Created guardian: {len(b)} bones, three attack animations, emissive crystal, two original sounds, physical totem board.')
