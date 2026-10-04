"""Rehearse interrupted inventory payments on marked QA accounts only."""
from pathlib import Path
import json, copy, subprocess, time, socket, nbtlib, hashlib
R=Path(__file__).resolve().parents[1]
S=R/'tools/test-runtime/social27/server'
assert (S/'.tecni-test-world').exists()
try:
    with socket.create_connection(('127.0.0.1',25569),1): pass
except OSError: pass
else: raise RuntimeError('Stop the isolated fixture before recovery rehearsal')
uid='3529de0a-6f14-344d-b341-a780ba951f1e' # E25CivicA, never a real account.
ledger=S/'world/tecnihardcore-social.json'
player=S/'world/playerdata'/f'{uid}.dat'
assert player.exists()
baseline=json.loads(ledger.read_text())
before={key:copy.deepcopy(baseline['data'][key]) for key in ['teams','truces','offers','parcels','quests']}
java=R/'client/runtime/jdk-17.0.20.1+1-jre/bin/java.exe'
results=[]
for i,phase in enumerate(['journal-before-inventory','inventory-before-commit','completed-commit']):
    state=json.loads(ledger.read_text())
    next_state=copy.deepcopy(state['data'])
    next_state['wallets'][uid]=state['data']['wallets'].get(uid,0)+1
    after=nbtlib.parse_nbt('[{Slot:0b,id:"minecraft:emerald",Count:'+str(7+i)+'b}]')
    if i>0:
        tag=nbtlib.load(player);tag['Inventory']=after;tag.save()
    if i==2:
        state={'data':next_state}
    else:
        state['pending']={'player':uid,'inventory':after.snbt(),'business':json.dumps(next_state),'reason':'isolated '+phase}
    ledger.write_text(json.dumps(state),encoding='utf8')
    log=S/'qa-results'/f'recovery-{phase}.log'
    with log.open('wb') as output:
        process=subprocess.Popen([str(java),'-Xms256M','-Xmx1100M','-Dtecni.testServer=true','-jar','fabric-server-launch.jar','nogui'],cwd=S,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            end=time.time()+90
            while time.time()<end:
                if process.poll() is not None:raise RuntimeError('Fixture exited: '+log.read_text(errors='replace')[-2000:])
                if 'Done (' in log.read_text(errors='replace'):break
                time.sleep(.25)
            else:raise RuntimeError('Startup timeout')
            recovered=json.loads(ledger.read_text())
            assert recovered.get('pending') is None
            assert recovered['data']==next_state,phase
            assert nbtlib.load(player)['Inventory']==after,phase
            (S/'qa-commands.txt').write_text('stop\n')
            process.wait(timeout=30)
            assert process.returncode==0
        finally:
            if process.poll() is None:process.terminate();process.wait(timeout=10)
    results.append({'phase':phase,'inventoryCount':7+i,'balance':next_state['wallets'][uid],'pendingCleared':True})
    print('PASS',phase,flush=True)
final=json.loads(ledger.read_text())
for key,value in before.items():assert final['data'][key]==value,key
out={'passed':True,'cases':results,'unrelatedRecordsPreserved':True,'player':'E25CivicA'}
(S/'qa-results/recovery27-result.json').write_text(json.dumps(out,indent=2))
print('PASS recovery: exact inventory and wallet after all three restart stages; no duplicate claim',flush=True)
