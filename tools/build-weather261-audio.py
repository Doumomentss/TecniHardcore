"""Smooth tornado wind and dry thunder, with no rain recording or percussive metal tones."""
from pathlib import Path
import hashlib,json,subprocess,wave
import numpy as np
import imageio_ffmpeg
root=Path(__file__).resolve().parents[1]
out=root/'tools/test-runtime/weather261';out.mkdir(parents=True,exist_ok=True)
assets=root/'custom_mods/hardcore/src/main/resources/assets/tecnihardcore'
dest=assets/'sounds/disaster';sr=32000;rng=np.random.default_rng(261)
def rumble(seconds,color,low,high):
    n=int(seconds*sr);f=np.fft.rfftfreq(n,1/sr);a=np.fft.rfft(rng.normal(size=n))
    a*=np.maximum(f,low)**(-color);a*=np.exp(-(f/high)**4);a[f<low]=0
    v=np.fft.irfft(a,n);return v/np.std(v)
def write(name,v,loop=False):
    if loop:
        count=sr;blend=np.linspace(0,1,count);v[:count]=v[-count:]*(1-blend)+v[:count]*blend;v=v[:-count]
    v=np.tanh(v)*.85;n=sr//40;v[:n]*=np.linspace(0,1,n);v[-n:]*=np.linspace(1,0,n)
    wav=out/(name+'.wav')
    with wave.open(str(wav),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes((v*32767).astype('<i2').tobytes())
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(),'-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(dest/(name+'.ogg'))],check=True)
    print(name,round(len(v)/sr,2),'seconds; peak',round(float(np.max(abs(v))),3))
t=np.arange(sr*20)/sr
wind=rumble(20,.95,24,1700)*(.33+.09*np.sin(t*.32)+.05*np.sin(t*.71))
wind+=rumble(20,1.15,20,190)*(.13+.04*np.sin(t*.45))
write('wind_smooth',wind,True)
for i in range(1,4):
    seconds=6+i*.5;t=np.arange(int(sr*seconds))/sr
    envelope=(1-np.exp(-t*4))*np.exp(-t*.45)*(1+.18*np.sin(t*1.8))
    dry=rumble(seconds,1.1,22,520)*envelope*.7
    write('wind_thunder_'+str(i),dry)
sounds=json.loads((assets/'sounds.json').read_text(encoding='utf8'))
sounds['disaster.wind']['sounds']=[{'name':'tecnihardcore:disaster/wind_smooth','stream':True}]
sounds['disaster.wind_thunder']={'sounds':['tecnihardcore:disaster/wind_thunder_'+str(i) for i in range(1,4)],'subtitle':'subtitles.tecnihardcore.disaster_thunder'}
(assets/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n',encoding='utf8')
credits=json.loads((assets/'AUDIO-CREDITS.json').read_text(encoding='utf8'))
credits['tornado261']='Original smooth filtered-noise wind and dry thunder synthesis; no rain field recording in tornado wind/thunder.'
credits['files'].update({p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in dest.glob('wind_*.ogg')})
(assets/'AUDIO-CREDITS.json').write_text(json.dumps(credits,indent=2)+'\n',encoding='utf8')
