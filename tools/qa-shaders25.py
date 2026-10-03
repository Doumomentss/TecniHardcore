"""Native screenshots of essential geometry with both pinned shader presets."""
from pathlib import Path
import time,subprocess,sys,atexit
R=Path(__file__).resolve().parents[1];B=R/'tools/test-runtime/expansion25';S=B/'server';G=B/'game'
def wait(fn,seconds=120):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if fn():return
  time.sleep(.2)
 raise TimeoutError('Native shader test timeout')
def command(*lines):
 p=S/'qa-commands.txt';wait(lambda:not p.exists());p.write_text('\n'.join(lines)+'\n');wait(lambda:not p.exists());time.sleep(.4)
def request(name):
 p=G/'qa-capture-request.txt';wait(lambda:not p.exists());p.write_text(name);wait(lambda:not p.exists());time.sleep(12 if name=='graphics-reload' else .5)
def capture(name):
 p=G/f'screenshots/sanctuary-{name}.png';old=p.stat().st_mtime if p.exists() else 0;request(name);wait(lambda:p.exists() and p.stat().st_mtime>old);print('Captured:',name,flush=True)
def graphics(mode):
 code=f"require('./launcher/graphics').apply('tools/test-runtime/expansion25/game','{mode}','installer_payload').catch(e=>{{console.error(e);process.exit(1)}})"
 subprocess.run(['node','-e',code],cwd=R,check=True)
if '--targeted' not in sys.argv:time.sleep(35)
actors=None
if '--targeted' in sys.argv:
 ready=B/'shader-actors-ready.json'
 if ready.exists():ready.unlink()
 actors=subprocess.Popen(['node','tools/qa-shader-actors25.cjs'],cwd=R)
 atexit.register(actors.terminate)
 wait(lambda:ready.exists())
 command('tecni vidas E25ShaderA 3','tecni vidas E25ShaderB 3','gamemode survival E25ShaderA','gamemode survival E25ShaderB','execute in tecnihardcore:eventos run tp E25ShaderA 10000.5 95 .5','execute in tecnihardcore:eventos run tp E25ShaderB 10012.5 95 .5','effect give E25ShaderA minecraft:resistance 600 255 true','effect give E25ShaderB minecraft:resistance 600 255 true')
command('tecni desastre detener todos','gamemode spectator E25View')
quick='--quick' in sys.argv
for mode in (['quality'] if quick else ['quality','ultra-quality']):
 graphics(mode);request('graphics-reload')
 for kind in (['electrica','meteoritos','terremoto'] if '--targeted' in sys.argv else ['tornado'] if quick else ['tornado','acida','electrica','meteoritos','terremoto']):
  command('execute in tecnihardcore:eventos run tp E25View '+('10000.5 195 245.5 180 0' if kind=='tornado' else '10000.5 104 32.5 180 12'),f'execute in tecnihardcore:eventos run tecni desastre iniciar {kind} 10000 95 0 96 60')
  time.sleep(13 if kind=='terremoto' else 11);capture(kind+'-'+mode+'-fixed');command('tecni desastre detener todos');time.sleep(4)
if not quick:
 request('fullscreen-toggle');command('execute in tecnihardcore:eventos run tp E25View 10000.5 195 245.5 180 0','execute in tecnihardcore:eventos run tecni desastre iniciar tornado 10000 95 0 96 60');time.sleep(11);capture('tornado-fullscreen-fixed');request('fullscreen-toggle');command('tecni desastre detener todos');time.sleep(4);capture('disasters-cleared-fixed')
graphics('optimized');request('graphics-reload')
