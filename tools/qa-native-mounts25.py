"""Native Minecraft visual/input tests against the marked localhost copied world only."""
from pathlib import Path
import json,time
ROOT=Path(__file__).resolve().parents[1]/'tools/test-runtime/expansion25'
SERVER=ROOT/'server'; GAME=ROOT/'game'; RESULTS=[]
def wait(test,seconds=90):
    end=time.monotonic()+seconds
    while time.monotonic()<end:
        if test():return
        time.sleep(.2)
    raise TimeoutError('QA condition did not become true')
def command(*lines):
    p=SERVER/'qa-commands.txt';wait(lambda:not p.exists());p.write_text('\n'.join(lines)+'\n');wait(lambda:not p.exists());time.sleep(.4)
def action(**values):
    p=GAME/'qa-action.json';wait(lambda:not p.exists());p.write_text(json.dumps(values));wait(lambda:not p.exists());time.sleep(.4)
def capture(name):
    p=GAME/'qa-capture-request.txt';wait(lambda:not p.exists());dest=GAME/f'screenshots/sanctuary-{name}.png';old=dest.stat().st_mtime if dest.exists() else 0;p.write_text(name);wait(lambda:dest.exists() and dest.stat().st_mtime>old)
def report(tag):
    command('qa25 report E25View '+tag);return json.loads((SERVER/f'qa-results/E25View-{tag}.json').read_text())
def store():return json.loads((SERVER/'world/tecnihardcore-expansion.json').read_text())
def check(name,ok,details=None):
    RESULTS.append(dict(name=name,ok=bool(ok),details=details));print(('PASS' if ok else 'FAIL')+': '+name,flush=True)
try:
    wait(lambda:'E25View joined the game' in (SERVER/'logs/latest.log').read_text(),180)
    command('tecni desastre detener todos','tecni vidas E25View 3','gamemode survival E25View','effect give E25View minecraft:resistance 600 255 true','execute in tecnihardcore:eventos run tp E25View 10000.5 95 12.5 180 0')
    command('clear E25View',*['tecni montura dar E25View '+str(t) for t in range(1,6)])
    action(type='view',perspective=2,particles=1)
    for tier in range(1,6):
        command('qa25 mount E25View '+str(tier));before=report('tier'+str(tier)+'-start');check(f'Tier {tier} mounted',before.get('mountTier')==tier,before)
        assert before.get('mountTier')==tier
        capture('mount-'+str(tier)+'-ground')
        action(type='drive',forward=0,side=0,vertical=1,boost=False,ticks=40);time.sleep(3)
        flying=report('tier'+str(tier)+'-flying');check(f'Tier {tier} server validates ascent',flying['y']>before['y']+5 and flying['lives']==3,{'beforeY':before['y'],'afterY':flying['y']})
        capture('mount-'+str(tier)+'-flight')
        command('qa25 mount-clone E25View','qa25 mount E25View '+str(tier));cloned=report('tier'+str(tier)+'-clone');check(f'Tier {tier} copied token cannot duplicate active creature',cloned['activeEntities']==1)
        action(type='drive',forward=1,side=0,vertical=0,boost=True,ticks=40);time.sleep(3)
        boosted=report('tier'+str(tier)+'-boost');record=store()['mounts'][before['mount']];check(f'Tier {tier} boost timestamp persisted',record['boostAt']>0 and abs(boosted['z']-flying['z'])>6)
        action(type='drive',forward=0,side=0,vertical=-1,boost=False,ticks=120);time.sleep(7)
        command('qa25 mount-interact E25View store');time.sleep(1)
        record=store()['mounts'][before['mount']];check(f'Tier {tier} grounded storage preserves health and boost',record['state']=='guardada' and record['health']==before['mountHealth'] and record['boostAt']>0,record)
        command('execute in tecnihardcore:eventos run tp E25View 10000.5 95 12.5 180 0')
    command('qa25 mount E25View 1','qa25 mount-damage E25View 1000');time.sleep(4)
    command('qa25 mount E25View 1');dead=report('permanent-death');check('Dead mount token cannot recreate creature',dead['activeEntities']==0 and 'mount' not in dead)
finally:
    (ROOT/'native-mount-results.json').write_text(json.dumps(RESULTS,indent=2))
if any(not row['ok'] for row in RESULTS):raise SystemExit(1)
