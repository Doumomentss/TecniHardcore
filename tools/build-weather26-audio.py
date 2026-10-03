"""Public-domain field recording plus original synthesis; mono, peak-limited game audio."""
from pathlib import Path
import hashlib,json,subprocess,wave
import numpy as np
import imageio_ffmpeg
R=Path(__file__).resolve().parents[1];S=R/'tools/test-runtime/weather26';A=R/'custom_mods/hardcore/src/main/resources/assets/tecnihardcore';D=A/'sounds/disaster';D.mkdir(parents=True,exist_ok=True)
source=S/'rain-and-thunder-original.ogg'
if not source.exists():
 import urllib.request
 S.mkdir(parents=True,exist_ok=True)
 with urllib.request.urlopen('https://upload.wikimedia.org/wikipedia/commons/4/42/Rain_and_thunder.ogg',timeout=30) as response:source.write_bytes(response.read())
assert hashlib.sha1(source.read_bytes()).hexdigest()=='5cee3290a09118a6d2fcdcbaa17f451f9ec09499'
ffmpeg=imageio_ffmpeg.get_ffmpeg_exe();sr=32000
raw=subprocess.check_output([ffmpeg,'-v','error','-i',str(source),'-f','f32le','-ac','1','-ar',str(sr),'-']);record=np.frombuffer(raw,np.float32).astype(np.float64)
rng=np.random.default_rng(260)
def noise(seconds,color=1):
 n=int(seconds*sr);samples=rng.normal(0,1,n);freq=np.fft.rfftfreq(n,1/sr);spectrum=np.fft.rfft(samples);spectrum*=1/np.maximum(24,freq)**color;spectrum[freq<20]=0;spectrum[freq>4000]=0;result=np.fft.irfft(spectrum,n);return result/max(1e-9,np.std(result))
def write(name,samples,loop=False):
 samples=np.tanh(samples)*.9
 if loop:
  n=min(sr//2,len(samples)//8);cross=np.linspace(0,1,n);samples[:n]=samples[-n:]*(1-cross)+samples[:n]*cross;samples=samples[:-n]
 n=min(sr//100,len(samples)//8);samples[:n]*=np.linspace(0,1,n);samples[-n:]*=np.linspace(1,0,n)
 wav=S/(name+'.wav')
 with wave.open(str(wav),'wb') as f:f.setnchannels(1);f.setsampwidth(2);f.setframerate(sr);f.writeframes((samples*32767).astype('<i2').tobytes())
 subprocess.run([ffmpeg,'-y','-v','error','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(D/(name+'.ogg'))],check=True)
 print(name,len(samples)/sr,'s',int(np.max(np.abs(samples))*100),'% peak')
field=record/max(.01,np.std(record));write('storm_real',field*.75,True);write('rain_real',field*.35,True)
n=12*sr;t=np.arange(n)/sr;wind=noise(12,.75)*(.3+.16*np.sin(t*.9)+.10*np.sin(t*2.4))+np.sin(t*2*np.pi*42)*.08;write('wind_real',wind,True)
quake=noise(8,1.1)*.48;qt=np.arange(len(quake))/sr;quake+=(np.sin(qt*2*np.pi*37)+.3*np.sin(qt*2*np.pi*71))*(.22+.15*np.sin(qt*2*np.pi/8));write('quake_real',quake,True)
at=np.arange(sr*5)/sr;frequency=480+400*(.5-.5*np.cos(at*2*np.pi/5));phase=2*np.pi*np.cumsum(frequency)/sr;alarm=np.sin(phase)*.4+np.sin(phase*2)*.13+np.sin(phase*3)*.08;alarm*=.65+.35*np.sin(at*2*np.pi*5)**2;write('alarm',alarm*1.5,True)
for i,second in enumerate([0,6,12],1):segment=record[int(second*sr):int(min(second+6,len(record)/sr)*sr)];write('thunder_real_'+str(i),segment/max(.01,np.std(segment))*.8)
sounds=json.loads((A/'sounds.json').read_text('utf8'))
for event,name in [('wind','wind_real'),('quake','quake_real'),('rain','rain_real'),('storm','storm_real'),('alarm','alarm')]:sounds['disaster.'+event]={'sounds':[{'name':'tecnihardcore:disaster/'+name,'stream':True}],'subtitle':'subtitles.tecnihardcore.disaster_'+event}
sounds['disaster.thunder']={'sounds':['tecnihardcore:disaster/thunder_real_'+str(i) for i in range(1,4)],'subtitle':'subtitles.tecnihardcore.disaster_thunder'}
(A/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n','utf8')
credits={'source':'https://commons.wikimedia.org/wiki/File:Rain_and_thunder.ogg','author':'Caesar','license':'Public domain (PD-self)','originalSha1':hashlib.sha1(source.read_bytes()).hexdigest(),'changes':'Converted to mono, compressed and excerpted into rain/storm/thunder game sounds. Wind, quake and alarm are original TecniHardcore synthesis.','files':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in D.glob('*real*.ogg')}}
(A/'AUDIO-CREDITS.json').write_text(json.dumps(credits,indent=2)+'\n','utf8')
