"""Original code-native geometry, pixel atlas, particles and synthesized ritual score.
No downloaded art/audio; deterministic, editable GeckoLib geometry and animations.
"""
from pathlib import Path
import json, math, random, wave, subprocess
from PIL import Image, ImageDraw
import numpy as np
import imageio_ffmpeg
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'custom_mods/hardcore/src/main/resources'
A=RES/'assets/tecnihardcore'
def js(p,d):
    p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2),'utf-8')
def cube(origin,size,material='metal',**kw):
    return dict(origin=origin,size=size,uv={'metal':[0,0],'gold':[128,0],'crystal':[0,128],'glow':[128,128]}[material],**kw)
bones=[dict(name='root',pivot=[0,0,0],cubes=[cube([-14,0,-14],[28,3,28]),cube([-12,3,-12],[24,2,24],'gold'),cube([-10,5,-10],[20,3,20]),cube([-11,8,-11],[22,1,22],'gold')])]
for i in range(4):
    a=i*math.pi/2+math.pi/4;x,z=math.cos(a)*11,math.sin(a)*11
    bones.append(dict(name=f'claw_{i}',parent='root',pivot=[x,6,z],rotation=[0,-i*90-45,0],cubes=[cube([x-2,6,z-2],[4,12,4]),cube([x-2.5,17,z-2.5],[5,2,5],'gold'),cube([x-1.5,19,z-1.5],[3,4,3],'glow')]))
for name,r,y,count in [('ring_outer',18,28,24),('ring_inner',13,28,20)]:
    pieces=[]
    for i in range(count):
        a=i*2*math.pi/count;x,z=math.cos(a)*r,math.sin(a)*r
        pieces.append(cube([x-2.3,y-.6,z-.7],[4.6,1.2,1.4],'gold',pivot=[x,y,z],rotation=[0,90-math.degrees(a),0]))
        if i%2==0:pieces.append(cube([x-1,y+.5,z-.65],[2,.6,1.3],'glow',pivot=[x,y,z],rotation=[0,90-math.degrees(a),0]))
    bones.append(dict(name=name,parent='root',pivot=[0,y,0],cubes=pieces))
crystal=[];fragments=[]
for j in range(10):
    width=2+min(j,9-j)*1.35
    fragments.append(dict(name=f'fragment_{j}',parent='crystal',pivot=[0,29,0],cubes=[cube([-width/2,20+j*1.8,-width/2],[width,1.8,width],'crystal',pivot=[0,29,0],rotation=[0,45,0])]))
crystal.extend([cube([-1.4,23,-1.4],[2.8,12,2.8],'glow',pivot=[0,29,0],rotation=[0,45,0]),cube([-4,28,-4],[8,1,8],'gold',pivot=[0,29,0],rotation=[0,45,0])])
bones.append(dict(name='crystal',parent='root',pivot=[0,29,0],cubes=crystal))
bones.extend(fragments)
js(A/'geo/santuario.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.santuario','texture_width':256,'texture_height':256,'visible_bounds_width':5,'visible_bounds_height':5,'visible_bounds_offset':[0,1.5,0]},'bones':bones}]})
js(A/'animations/santuario.animation.json',{'format_version':'1.8.0','animations':{'animation.sanctuary.idle':{'loop':True,'animation_length':4,'bones':{'crystal':{'position':{'0':[0,0,0],'2':[0,1.2,0],'4':[0,0,0]}}}}}})
rng=random.Random(20261002);atlas=Image.new('RGBA',(256,256));glow=Image.new('RGBA',atlas.size)
palettes=[(0,0,(26,38,44)),(128,0,(159,111,39)),(0,128,(23,129,102)),(128,128,(75,222,170))]
for ox,oy,base in palettes:
    for y in range(128):
        for x in range(128):
            grain=rng.randint(-8,8);edge=20 if x%16 in (0,1) or y%16 in (0,1) else 0
            facet=(18 if (x+y)%32<8 else -8) if oy else 0
            color=tuple(max(0,min(255,v+grain+edge+facet)) for v in base)+(255,)
            atlas.putpixel((ox+x,oy+y),color)
            if ox==128 and oy==128:glow.putpixel((ox+x,oy+y),color)
            elif oy==128 and (x+y)%32<5:glow.putpixel((ox+x,oy+y),(40,190,145,90))
for p,im in [(A/'textures/block/santuario.png',atlas),(A/'textures/block/santuario_glowmask.png',glow)]:p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
for name in ['soul_shard','soul_rune','relic_ember']:
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    if name=='soul_shard':d.polygon([(8,1),(12,8),(8,15),(4,8)],fill=(210,255,241,255));d.line([(8,3),(8,12)],fill='white',width=2)
    elif name=='soul_rune':d.line([(8,1),(13,8),(8,14),(3,8),(8,1)],fill='white');d.line([(8,4),(8,11)],fill='white',width=2);d.line([(5,8),(11,8)],fill='white')
    else:d.polygon([(8,0),(9,6),(15,8),(9,9),(8,15),(6,9),(0,8),(6,6)],fill='white')
    p=A/f'textures/particle/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p);js(A/f'particles/{name}.json',{'textures':[f'tecnihardcore:{name}']})
js(RES/'data/tecnihardcore/worldgen/configured_feature/sanctuary.json',{'type':'tecnihardcore:sanctuary','config':{}})
js(RES/'data/tecnihardcore/worldgen/placed_feature/sanctuary.json',{'feature':'tecnihardcore:sanctuary','placement':[{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'}]})
js(A/'blockstates/santuario.json',{'variants':{'':{'model':'tecnihardcore:block/santuario'}}})
js(A/'models/block/santuario.json',{'textures':{'particle':'tecnihardcore:block/santuario'}})
for lang in ['es_es','es_mx','es_ar','en_us']:
    p=A/f'lang/{lang}.json';data=json.loads(p.read_text('utf-8'));data['block.tecnihardcore.santuario']='Santuario de las Almas' if lang!='en_us' else 'Sanctuary of Souls';js(p,data)
sounds={};rate=44100
for name,duration,notes in [('wake',3.8,[164.81,246.94,329.63]),('channel',6,[130.81,196,261.63,392]),('complete',4,[261.63,329.63,392,523.25]),('cancel',1.7,[196,155.56,130.81])]:
    t=np.arange(int(rate*duration))/rate;data=np.zeros_like(t)
    for j,f in enumerate(notes):
        start=0 if name=='channel' else j*.25;tt=np.maximum(0,t-start)
        env=np.exp(-tt*(.28 if name=='channel' else .9))*(t>=start)
        data+=(np.sin(2*np.pi*f*tt)+.28*np.sin(2*np.pi*f*2*tt)+.12*np.sin(2*np.pi*f*3.01*tt))*env/(len(notes)*1.6)
    if name=='channel':data*=.65+.15*np.sin(2*np.pi*t/3)
    else:data+=np.random.default_rng(7).normal(0,.035,len(t))*np.exp(-t*3)
    fade=np.minimum(1,t/.1)*np.minimum(1,(duration-t)/.4);data=np.clip(data*fade,-.8,.8)
    audio=A/'sounds/sanctuary';audio.mkdir(parents=True,exist_ok=True);wav=audio/f'{name}.wav'
    with wave.open(str(wav),'wb') as out:out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate);out.writeframes((data*32767).astype('<i2').tobytes())
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(),'-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(audio/f'{name}.ogg')],check=True);wav.unlink()
    sounds[f'sanctuary.{name}']={'sounds':[{'name':f'tecnihardcore:sanctuary/{name}','stream':False}],'subtitle':f'subtitles.tecnihardcore.{name}'}
js(A/'sounds.json',sounds)
for lang in ['es_es','es_mx','es_ar','en_us']:
    p=A/f'lang/{lang}.json';data=json.loads(p.read_text('utf-8'));data.update({f'subtitles.tecnihardcore.{k}':v for k,v in {'wake':'El santuario despierta','channel':'Las almas convergen','complete':'Un alma regresa','cancel':'El ritual se desvanece'}.items()});js(p,data)
print('Built original sanctuary: %d bones, %d cubes, emissive atlas, 3 particles and 4 sounds'%(len(bones),sum(len(b.get('cubes',[])) for b in bones)))
