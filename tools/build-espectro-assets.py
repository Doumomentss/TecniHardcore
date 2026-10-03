"""Original procedural geometry and mono positional sound for TecniHardcore 2.4."""
from pathlib import Path
import json,math,wave,subprocess,random
import numpy as np
from PIL import Image,ImageDraw
import imageio_ffmpeg
A=Path(__file__).resolve().parents[1]/'custom_mods/hardcore/src/main/resources/assets/tecnihardcore'
def js(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2,ensure_ascii=False),'utf-8')
def cube(o,s,uv=(0,0),**kw):return dict(origin=o,size=s,uv=list(uv),**kw)
b=[dict(name='body',pivot=[0,38,0],cubes=[cube([-9,26,-5],[18,22,10]),cube([-10,42,-6],[20,6,12],(128,0)),cube([-5,33,-8],[10,10,3],(128,128),pivot=[0,38,-7],rotation=[0,0,45]),cube([-12,22,-5],[8,9,10],rotation=[0,0,-20]),cube([4,22,-5],[8,9,10],rotation=[0,0,20])]),dict(name='head',parent='body',pivot=[0,48,0],cubes=[cube([-5,48,-4],[10,12,8]),cube([-4,54,-5],[8,2,1],(128,128)),cube([-7,59,-3],[3,12,4],(0,128),pivot=[-6,59,0],rotation=[0,0,-25]),cube([4,59,-3],[3,12,4],(0,128),pivot=[6,59,0],rotation=[0,0,25])])]
for sign,name in [(-1,'left'),(1,'right')]:
 x=sign*15
 b.append(dict(name=name+'_arm',parent='body',pivot=[x,45,0],cubes=[cube([x-5,37,-5],[10,11,10],(128,0)),cube([x-3,15,-3],[6,24,6]),cube([x-4,8,-4],[8,9,8],(0,128)),cube([x-2,1,-3],[4,8,6],(128,128))]))
 b.append(dict(name=name+'_leg',pivot=[sign*6,26,0],cubes=[cube([sign*6-4,8,-4],[8,19,8]),cube([sign*6-5,0,-7],[10,9,13],(128,0))]))
 wings=[]
 for i in range(6):
  x=sign*(12+i*4);y=48-i*3
  wings.append(cube([x-2,y-5,4],[4,24-i*2,4],(0,128),pivot=[x,y+4,6],rotation=[-15,sign*25,sign*(22+i*7)]))
 b.append(dict(name=name+'_wing',parent='body',pivot=[sign*10,45,5],cubes=wings))
for i in range(4):
 a=i*math.pi/2;x,z=math.cos(a)*16,math.sin(a)*16;b.append(dict(name='orbit_'+str(i),parent='body',pivot=[0,38,0],cubes=[cube([x-2,36,z-2],[4,10,4],(128,128),pivot=[x,40,z],rotation=[0,45,25])]))
js(A/'geo/espectro.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.espectro','texture_width':256,'texture_height':256,'visible_bounds_width':9,'visible_bounds_height':7,'visible_bounds_offset':[0,3,0]},'bones':b}]})
r=random.Random(24);atlas=Image.new('RGBA',(256,256));glow=Image.new('RGBA',atlas.size)
for ox,oy,base in [(0,0,(34,25,51)),(128,0,(164,119,53)),(0,128,(112,58,175)),(128,128,(215,137,255))]:
 for y in range(128):
  for x in range(128):
   n=r.randrange(-15,16)+(22 if oy and (x+y)%22<3 else 0);c=tuple(max(0,min(255,a+n)) for a in base)+(255,);atlas.putpixel((ox+x,oy+y),c)
   if (ox,oy)==(128,128):glow.putpixel((ox+x,oy+y),c)
atlas.save(A/'textures/entity/espectro.png');glow.save(A/'textures/entity/espectro_glowmask.png')
p=A/'animations/custodio.animation.json';d=json.loads(p.read_text());an=d['animations']
an['animation.custodio.transform']={'loop':False,'animation_length':6,'bones':{'body':{'rotation':{'0':[0,0,0],'1':[45,0,0],'2.5':[65,0,0],'3':[0,0,0],'4.5':[0,180,0],'6':[0,360,0]},'position':{'0':[0,0,0],'2.5':[0,-12,0],'3':[0,4,0],'6':[0,0,0]}},'left_arm':{'rotation':{'0':[0,0,0],'2':[30,0,-60],'4':[-70,0,-30],'6':[0,0,0]}},'right_arm':{'rotation':{'0':[0,0,0],'2':[30,0,60],'4':[-70,0,30],'6':[0,0,0]}}}}
an['animation.custodio.melee']={'loop':False,'animation_length':.5,'bones':{'right_arm':{'rotation':{'0':[-90,0,20],'.3':[35,0,-10],'.5':[0,0,0]}}}}
for name in ['idle','walk','cast','slam']:
 an['animation.custodio.'+name]['bones'].update({'left_wing':{'rotation':[0,'math.sin(query.anim_time * 90) * 12',-8]},'right_wing':{'rotation':[0,'-math.sin(query.anim_time * 90) * 12',8]}})
# Release motion coincides with the normalized windup in every phase.
an['animation.custodio.slam']['bones']['left_arm']['rotation']={'0':[0,0,0],'.8':[-145,0,-15],'1.55':[-145,0,-15],'1.65':[20,0,0],'3.5':[0,0,0]}
an['animation.custodio.slam']['bones']['right_arm']['rotation']={'0':[0,0,0],'.8':[-145,0,15],'1.55':[-145,0,15],'1.65':[20,0,0],'3.5':[0,0,0]}
js(p,d)
sounds=json.loads((A/'sounds.json').read_text());out=A/'sounds/boss';out.mkdir(exist_ok=True)
spec=[('melee',75,.45),('warning',390,.85),('prison',180,1.3),('volley',680,.65),('phase',95,2),('transform',60,6),('death',110,2.6)]
for i,(name,freq,duration) in enumerate(spec):
 rate=44100;t=np.arange(int(rate*duration))/rate;rng=np.random.default_rng(240+i)
 chirp=freq*t+(25 if name in ['warning','transform','volley'] else -8)*t*t
 tone=np.sin(2*np.pi*chirp)*.32+np.sin(2*np.pi*chirp*1.5)*.16+rng.normal(0,.04,len(t))
 envelope=np.minimum(1,t/.025)*np.minimum(1,(duration-t)/.15)*np.exp(-t/(duration*.7));audio=np.clip(tone*envelope,-.95,.95)
 wav=out/(name+'.wav')
 with wave.open(str(wav),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes((audio*32767).astype('<i2').tobytes())
 subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(),'-v','error','-y','-i',str(wav),str(out/(name+'.ogg'))],check=True);wav.unlink();sounds['boss.'+name]={'sounds':['tecnihardcore:boss/'+name],'subtitle':'subtitles.tecnihardcore.boss_'+name}
js(A/'sounds.json',sounds)
icon=Image.new('RGBA',(18,18));dr=ImageDraw.Draw(icon);dr.ellipse((1,1,16,16),outline='#e7a9ff',width=2);dr.line((4,4,13,13),fill='#b379ed',width=2);dr.line((4,13,13,4),fill='#b379ed',width=2);(A/'textures/mob_effect').mkdir(exist_ok=True);icon.save(A/'textures/mob_effect/rooted.png')
for p in (A/'lang').glob('*.json'):
 d=json.loads(p.read_text());d['effect.tecnihardcore.rooted']='Prisión del núcleo'
 for name,_,_ in spec:d['subtitles.tecnihardcore.boss_'+name]={'melee':'Golpe de coraza','warning':'Runas de advertencia','prison':'Cadenas del núcleo','volley':'Lanzas de cristal','phase':'El custodio despierta','transform':'El núcleo se reconstruye','death':'El espectro se disipa'}[name]
 js(p,d)
print('Espectro: original geometry, emissive atlas, transformation and seven mono sounds created.')
