"""Original 30-second welcoming cue, synthesized without third-party samples."""
from pathlib import Path
import json, subprocess, tempfile, wave
import numpy as np
import imageio_ffmpeg

root=Path(__file__).resolve().parents[1]
assets=root/'custom_mods/hardcore/src/main/resources/assets/tecnihardcore'
target=assets/'sounds/intro/welcome.ogg';target.parent.mkdir(parents=True,exist_ok=True)
rate=32000;time=np.arange(rate*30,dtype=np.float64)/rate
rng=np.random.default_rng(290)
music=np.zeros_like(time)
def bell(start,frequency,volume=.19,decay=3.5):
    global music
    t=time-start;active=t>=0
    env=np.zeros_like(t);env[active]=(1-np.exp(-t[active]*34))*np.exp(-t[active]/decay)
    color=np.sin(2*np.pi*frequency*t)+.32*np.sin(2*np.pi*frequency*2.01*t)+.13*np.sin(2*np.pi*frequency*3.96*t)
    music+=volume*env*color
def pad(start,end,frequencies,volume):
    global music
    attack=np.clip((time-start)/2,0,1);release=np.clip((end-time)/3,0,1)
    env=np.minimum(attack,release)
    voice=sum(np.sin(2*np.pi*f*time+.25*np.sin(time*.23+i))*1/(1+i*.35) for i,f in enumerate(frequencies))
    music+=volume*env*voice
pad(0,18,[110,164.81,220,329.63],.048)
pad(12,30,[146.83,220,293.66,440],.054)
for start,frequency in [(1,440),(3,554.37),(5,659.25),(7,880),(12,587.33),(14,739.99),(16,880),(20,440),(21,587.33),(22,739.99),(24,987.77),(26,1174.66)]:bell(start,frequency,.14 if start<20 else .2)
noise=rng.normal(size=len(time));smooth=np.convolve(noise,np.ones(190)/190,mode='same')
wind=(.015+.01*np.sin(time*.4))*smooth*np.minimum(1,time/2)*np.minimum(1,(30-time)/2)
music+=wind
music*=np.minimum(1,time/1.3)*np.minimum(1,(30-time)/2)
music=np.tanh(music*1.8)*.78
with tempfile.TemporaryDirectory() as tmp:
    wav=Path(tmp)/'welcome.wav'
    with wave.open(str(wav),'wb') as w:
        w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes((music*32767).astype('<i2').tobytes())
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(),'-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(target)],check=True)
sounds=json.loads((assets/'sounds.json').read_text(encoding='utf-8'))
sounds['intro.welcome']={'sounds':[{'name':'tecnihardcore:intro/welcome','stream':True}],'subtitle':'subtitles.tecnihardcore.intro_welcome'}
(assets/'sounds.json').write_text(json.dumps(sounds,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print(target,target.stat().st_size)
