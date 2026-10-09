"""Assign the three spawn skins without altering NPC dialogue or player data.

Run only with the target server stopped. Skins are streamed by the existing
2.7.0 protocol, so this server-side correction does not require a new launcher.
"""
from pathlib import Path
from PIL import Image
import argparse, hashlib, json, shutil, socket, datetime, os

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--server', type=Path, default=ROOT/'server')
args=parser.parse_args()
server=args.server.resolve()
properties={line.split('=',1)[0]:line.split('=',1)[1] for line in
            (server/'server.properties').read_text(encoding='utf8').splitlines()
            if '=' in line and not line.startswith('#')}
with socket.socket() as connection:
    connection.settimeout(1)
    if connection.connect_ex(('127.0.0.1',int(properties.get('server-port','25565'))))==0:
        raise SystemExit('Stop this server before updating the NPC configuration.')
config=server/'config/tecnihardcore/npcs.json'
before=json.loads(config.read_text(encoding='utf8'))
after=json.loads(json.dumps(before))
world=server/properties.get('level-name','world')
if not world.is_dir():raise SystemExit('World not found; refusing to create a replacement world.')
cache=world/'tecni-npc-skins'
cache.mkdir(exist_ok=True)
manifest=json.loads((ROOT/'assets/npc-skins/manifest.json').read_text(encoding='utf8'))
changed=[]
for name,hash in manifest.items():
    data=(ROOT/'assets/npc-skins'/f'{name}.png').read_bytes()
    assert hashlib.sha256(data).hexdigest()==hash
    with Image.open(ROOT/'assets/npc-skins'/f'{name}.png') as image:
        assert image.size==(64,64) and image.mode=='RGBA'
        image.verify()
    assert len(data)<=65536
    destination=cache/(hash+'.png')
    destination.write_bytes(data)
    assert hashlib.sha256(destination.read_bytes()).hexdigest()==hash
    if name not in after:continue
    # Preserve any skin the administrator has already assigned.
    if after[name].get('skinHash') and after[name]['skinHash']!=hash:continue
    after[name]['skinHash']=hash
    changed.append(name)
if after!=before:
    stamp=datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    shutil.copy2(config,config.with_name(f'npcs.before-custom-skins-{stamp}.json'))
    temporary=config.with_suffix('.json.tmp')
    with temporary.open('w',encoding='utf8') as stream:
        stream.write(json.dumps(after,ensure_ascii=False,indent=2)+'\n')
        stream.flush();os.fsync(stream.fileno())
    os.replace(temporary,config)
for name in before:
    assert {k:v for k,v in before[name].items() if k!='skinHash'}=={
        k:v for k,v in after[name].items() if k!='skinHash'}
proof={'passed':True,'server':str(server),'assigned':changed,'sha256':manifest,
       'dialoguesPositionsAndRolesUnchanged':True,'requiresClientUpdate':False}
(ROOT/'assets/npc-skins/installation-result.json').write_text(json.dumps(proof,indent=2),encoding='utf8')
print(json.dumps(proof))
