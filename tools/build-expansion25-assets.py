"""Original editable crystal beasts, storm atlases and positional audio. No third-party assets."""
from pathlib import Path
import json,math,random,wave,subprocess
from PIL import Image,ImageDraw
import numpy as np
import imageio_ffmpeg
A=Path(__file__).resolve().parents[1]/'custom_mods/hardcore/src/main/resources/assets/tecnihardcore'
def js(path,data):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
colors=[(82,202,210),(229,223,189),(76,213,133),(173,106,230),(100,161,255)]
def cube(o,s,tile=0,**kw):
 uv={face:{'uv':[tile*64+4,4],'uv_size':[24,24]} for face in ['north','south','east','west','up','down']}
 return dict(origin=o,size=s,uv=uv,**kw)
def bone(name,pivot,cubes,parent='body'):
 return dict(name=name,pivot=pivot,cubes=cubes,**({'parent':parent} if parent else {}))
for tier,color in enumerate(colors,1):
 rng=random.Random(250+tier);atlas=Image.new('RGBA',(256,128));glow=Image.new('RGBA',atlas.size)
 for tile,base in enumerate([(26,35,46),(181,138,65),color,tuple(min(255,x+45) for x in color)]):
  for y in range(128):
   for x in range(64):
    edge=min(x%32,31-x%32,y%32,31-y%32);noise=rng.randrange(-8,9);shine=28 if (x+y)%29<2 else 0;c=tuple(max(0,min(255,v+noise+shine+(12 if edge<2 else 0))) for v in base)+(255,);atlas.putpixel((x+tile*64,y),c)
    if tile==3 or tile==2 and (x+y)%29<2:glow.putpixel((x+tile*64,y),c)
 (A/'textures/entity').mkdir(parents=True,exist_ok=True);atlas.save(A/f'textures/entity/mount_{tier}.png');glow.save(A/f'textures/entity/mount_{tier}_glowmask.png')
 b=[]
 if tier==1:
  body=[cube([-14,15,-18],[28,5,38],2),cube([-11,20,-12],[22,3,28]),cube([-8,23,-10],[16,2,18],1),cube([-4,22,-15],[8,7,8],3,pivot=[0,25,-11],rotation=[0,0,45])]
 elif tier==2:
  body=[cube([-7,13,-13],[14,13,28]),cube([-5,24,-8],[10,3,18],1),cube([-4,18,-14],[8,9,9],2)]
 else:
  body=[cube([-10,15,-17],[20,13,35]),cube([-12,25,-11],[24,4,18],1),cube([-7,28,-8],[14,6,14],2),cube([-4,30,-6],[8,8,8],3,pivot=[0,34,-2],rotation=[0,0,45])]
 b.append(bone('body',[0,20,0],body,None))
 if tier>1:
  neck=8 if tier==2 else 14 if tier==5 else 7
  b.append(bone('neck',[0,23,-14],[cube([-5,21,-20],[10,neck,10],2)]))
  headY=29 if tier==2 else 35 if tier==5 else 28
  head=[cube([-6,headY,-28],[12,9,13]),cube([-5,headY-2,-34],[10,6,9],1),cube([-6.2,headY+4,-27],[1,2,4],3),cube([5.2,headY+4,-27],[1,2,4],3)]
  if tier==2:head.append(cube([-3,headY,-38],[6,3,9],1))
  else:
   for sign in [-1,1]:head.append(cube([sign*7-1,headY+6,-23],[2,12,3],2,pivot=[sign*6,headY+6,-21],rotation=[-20,0,sign*25]))
  b.append(bone('head',[0,headY,-20],head,'neck'))
 for sign,name in [(-1,'left'),(1,'right')]:
  wing=[]
  for i in range(6):
   if tier==1:size=[8,2,34-i*4];o=[sign*(13+i*7)-(8 if sign<0 else 0),18+i*.4,-13+i*2]
   else:size=[7,2,30-i*3];o=[sign*(8+i*7)-(7 if sign<0 else 0),24-i*.5,-8+i*3]
   wing.append(cube(o,size,2,pivot=[sign*(8+i*7),24,0],rotation=[0,sign*(8+i*4),sign*(4+i*3)]))
   wing.append(cube([o[0],o[1]+1.5,o[2]],[size[0],1,2],1))
  b.append(bone(name+'_wing',[sign*9,22,0],wing))
  if tier>1:
   for pair in range(2 if tier in [3,5] else 1):
    z=-9 if pair==0 and tier in [3,5] else 11
    b.append(bone(name+'_leg'+str(pair),[sign*8,18,z],[cube([sign*8-2,6,z-2],[4,13,5]),cube([sign*8-3,3,z-7],[6,4,11],1)]))
 tail=[cube([-3,16,16],[6,5,22],2),cube([-2,17,35],[4,3,24],2),cube([-4,17,57],[8,3,11],3,pivot=[0,18,61],rotation=[0,0,25])]
 b.append(bone('tail',[0,19,15],tail))
 if tier>=4:
  for i in range(5):b.append(bone('spine_'+str(i),[0,30,-10+i*6],[cube([-2,29,-10+i*6],[4,8+i%2*3,4],3,pivot=[0,31,-8+i*6],rotation=[15,0,45])]))
 js(A/f'geo/mount_{tier}.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':f'geometry.mount_{tier}','texture_width':256,'texture_height':128,'visible_bounds_width':10,'visible_bounds_height':6,'visible_bounds_offset':[0,2,0]},'bones':b}]})
animations={}
for clip in ['idle','fly','boost','land','hurt','death']:
 speed={'idle':90,'fly':380,'boost':650,'land':180,'hurt':240,'death':60}[clip];amplitude=3 if clip=='idle' else 26 if clip=='fly' else 38
 bones={'body':{'position':[0,f'math.sin(query.anim_time * {speed}) * '+('1' if clip=='idle' else '2'),0]},'left_wing':{'rotation':[0,0,f'math.sin(query.anim_time * {speed}) * {amplitude} - 8']},'right_wing':{'rotation':[0,0,f'-math.sin(query.anim_time * {speed}) * {amplitude} + 8']},'tail':{'rotation':[0,f'math.sin(query.anim_time * {speed/2}) * 12',0]}}
 if clip=='hurt':bones['body']['rotation']={'0':[0,0,0],'.12':[0,0,-12],'.25':[0,0,8],'.5':[0,0,0]}
 if clip=='death':bones={'body':{'position':{'0':[0,0,0],'1':[0,-7,0],'3':[0,-14,0]},'rotation':{'0':[0,0,0],'1':[0,0,20],'3':[0,0,85]}},'left_wing':{'rotation':{'0':[0,0,0],'3':[0,0,-60]}},'right_wing':{'rotation':{'0':[0,0,0],'3':[0,0,60]}}}
 animations['animation.mount.'+clip]={'loop':clip not in ['hurt','death','land'],'animation_length':3 if clip=='death' else 1,'bones':bones}
js(A/'animations/mount.animation.json',{'format_version':'1.8.0','animations':animations})
effects=A/'textures/effects';effects.mkdir(parents=True,exist_ok=True)
for name in ['tornado','meteor']:
 im=Image.new('RGBA',(256,256));rng=random.Random(251)
 for y in range(256):
  for x in range(256):
   cloud=(math.sin(x*.08+y*.05)+math.sin(x*.035-y*.1)+math.sin(x*.18+y*.13))*.16+.5;noise=rng.uniform(-.07,.07)
   if name=='tornado':value=int(90+80*(cloud+noise));im.putpixel((x,y),(value,value+4,min(255,value+9),int(145+90*cloud)))
   else:im.putpixel((x,y),(int(180+70*cloud),int(40+130*cloud),int(15+35*cloud),255))
 im.save(effects/(name+'.png'))
for name,color in [('invocacion_cristal',(90,217,233)),('trofeo_evento',(245,183,64))]:
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.ellipse((5,4,26,25),outline=(24,31,45),width=4);d.ellipse((6,5,25,24),outline=(190,144,61),width=2);d.polygon([(16,2),(24,12),(16,26),(8,12)],fill=color,outline=(223,255,252));d.polygon([(16,3),(16,22),(10,12)],fill=tuple(int(v*.65) for v in color));d.rectangle((12,25,20,29),fill=(190,144,61));(A/'textures/item').mkdir(exist_ok=True);im.save(A/f'textures/item/{name}.png');js(A/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'tecnihardcore:item/'+name}})
sounds=json.loads((A/'sounds.json').read_text('utf8'));directory=A/'sounds/disaster';directory.mkdir(parents=True,exist_ok=True)
for i,(name,seconds) in enumerate([('wind',3),('quake',2.5),('rain',3),('thunder',2),('impact',1.7)]):
 rate=44100;t=np.arange(int(rate*seconds))/rate;noise=np.random.default_rng(250+i).normal(0,.35,len(t));smooth=np.convolve(noise,np.ones(20)/20,mode='same');audio=smooth*(.8+.2*np.sin(t*13))+np.sin(t*2*np.pi*(45+i*8))*.14
 if name in ['thunder','impact']:audio*=np.exp(-t*2)
 audio*=np.minimum(1,t/.04)*np.minimum(1,(seconds-t)/.2);wav=directory/(name+'.wav')
 with wave.open(str(wav),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes((np.clip(audio,-.95,.95)*32767).astype('<i2').tobytes())
 subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(),'-v','error','-y','-i',str(wav),str(directory/(name+'.ogg'))],check=True);wav.unlink();sounds['disaster.'+name]={'sounds':['tecnihardcore:disaster/'+name],'subtitle':'subtitles.tecnihardcore.disaster_'+name}
js(A/'sounds.json',sounds)
for path in (A/'lang').glob('*.json'):
 data=json.loads(path.read_text('utf8'));data.update({'entity.tecnihardcore.bestia_cristal':'Bestia de cristal','item.tecnihardcore.invocacion_cristal':'Invocación de cristal','item.tecnihardcore.trofeo_evento':'Trofeo de evento','category.tecnihardcore':'TecniHardcore','key.tecnihardcore.mount_up':'Montura: ascender','key.tecnihardcore.mount_down':'Montura: descender','key.tecnihardcore.mount_boost':'Montura: impulso'})
 for name,text in [('wind','El tornado ruge'),('quake','La tierra tiembla'),('rain','Lluvia corrosiva'),('thunder','Descarga eléctrica'),('impact','Impacto de meteorito')]:data['subtitles.tecnihardcore.disaster_'+name]=text
 js(path,data)
print('Five original beasts, six animation clips, emissive atlases, storm textures and five mono sounds generated.')

particle=Image.new('RGBA',(16,16));pd=ImageDraw.Draw(particle);pd.line((8,1,8,12),fill=(255,222,35,240),width=2);pd.ellipse((5,10,10,15),fill=(216,192,35,205));(A/'textures/particle').mkdir(exist_ok=True);particle.save(A/'textures/particle/acid_drop.png');js(A/'particles/acid_drop.json',{'textures':['tecnihardcore:acid_drop']})
