"""Pinned server dependencies/configuration; never modifies a running server."""
from pathlib import Path
import argparse, json, hashlib, urllib.request, shutil, re, socket
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(); parser.add_argument('server'); parser.add_argument('--experimental-grim',action='store_true',help='Only isolated fixtures; known incompatible with this full modpack'); args=parser.parse_args()
destination=Path(args.server).resolve()
if destination!=root/'server' and not (destination/'.tecni-test-world').exists():
    raise SystemExit('An isolated-world marker is required outside production.')
properties=(destination/'server.properties').read_text()
port=int(re.search(r'^server-port=(\d+)$',properties,re.M).group(1))
try:
    with socket.create_connection(('127.0.0.1',port),timeout=1):pass
except OSError:pass
else:raise SystemExit('Stop this server cleanly before changing its security files.')
if not args.experimental_grim and any((destination/'mods').glob('grimac-*.jar')):
    raise SystemExit('Move the experimental Grim JAR out of mods while stopped: incompatible with this full modpack.')
lock=json.loads((root/'tools/security27-lock.json').read_text())
if args.experimental_grim and destination==root/'server':raise SystemExit('Grim cannot be deployed to this production modpack: incompatible item/inventory registries.')
(destination/'mods').mkdir(exist_ok=True)
for item in lock['server']:
    if item['name'].startswith('grimac-') and not args.experimental_grim:continue
    cached=root/'tools/test-runtime/security27'/item['name']
    data=cached.read_bytes() if cached.exists() else urllib.request.urlopen(urllib.request.Request(item['url'],headers={'User-Agent':'TecniHardcore/2.7.0'}),timeout=60).read()
    assert len(data)==item['bytes'] and hashlib.sha512(data).hexdigest()==item['sha512'] and hashlib.sha256(data).hexdigest()==item['sha256'], item['name']
    (destination/'mods'/item['name']).write_bytes(data)
configs=destination/'config'; configs.mkdir(exist_ok=True)
antixray='''# TecniHardcore: efficient mode 1. Only concealed ores are replaced in packets.
enabled = false
[overworld]
enabled = true
engineMode = 1
maxBlockHeight = 320
updateRadius = 2
lavaObscures = false
hiddenBlocks = ["#c:ores", "coal_ore", "deepslate_coal_ore", "copper_ore", "deepslate_copper_ore", "iron_ore", "deepslate_iron_ore", "gold_ore", "deepslate_gold_ore", "redstone_ore", "deepslate_redstone_ore", "lapis_ore", "deepslate_lapis_ore", "diamond_ore", "deepslate_diamond_ore", "emerald_ore", "deepslate_emerald_ore", "raw_copper_block", "raw_iron_block", "raw_gold_block"]
[the_nether]
enabled = true
engineMode = 1
maxBlockHeight = 256
updateRadius = 2
lavaObscures = true
hiddenBlocks = ["ancient_debris", "nether_quartz_ore", "nether_gold_ore", "gilded_blackstone"]
'''
overworld_ores=[f'simpleores:{prefix}{ore}_ore' for ore in ['tin','mythril','adamantium'] for prefix in ['', 'deepslate_']]
nether_ores=[f'nether_ores_reborn:{ore}_ore' for ore in ['coal','copper','diamond','emerald','gold','iron','lapis','redstone']]+['simpleores:onyx_ore','simpleores:basalt_onyx_ore']
lines=antixray.splitlines(); seen=0
for index,line in enumerate(lines):
    if line.startswith('hiddenBlocks = '):
        existing=json.loads(line.split(' = ',1)[1]);existing.extend(overworld_ores if seen==0 else nether_ores);seen+=1
        lines[index]='hiddenBlocks = '+json.dumps(existing)
antixray='\n'.join(lines)+'\n'
files={configs/'antixray.toml':antixray}
defaults=root/'tools/security27-configs'
config=(defaults/'grim-config.yml').read_text()
# Modded weapon reaches and custom physics first run in diagnostic mode.
config=re.sub(r'(immediate-setback-threshold|max-advantage|setbackvl|cancelvl|setback-violation-threshold):[^\n]*',r'\1: -1',config)
config=config.replace('block-impossible-hits: true','block-impossible-hits: false').replace('check-for-updates: true','check-for-updates: false').replace('server-name: Prison','server-name: TecniHardcore')
config=config.replace('history:\n    enabled: true','history:\n    enabled: false')
files[configs/'grimac/config.yml']=config
punishments=(defaults/'grim-punishments.yml').read_text()
punishments='\n'.join(line for line in punishments.splitlines() if '[webhook]' not in line and '[proxy]' not in line)+'\n'
files[configs/'grimac/punishments.yml']=punishments
fiw=json.loads((defaults/'fiw.json').read_text())
fiw['timeout_seconds']=30
fiw['kick_message']='Cliente no permitido. Quita los mods de trampas y usa el launcher oficial TecniHardcore.'
fiw['timeout_message']='No se pudo verificar el cliente. Actualiza el launcher TecniHardcore y vuelve a entrar.'
fiw['detection']['preset']='custom'
fiw['detection']['block']['fullbright']=False # Shader/brightness controls remain allowed.
fiw['detection']['banned_mods']=['wurst','meteor-client','aristois','bleachhack','inertia','xray','advanced-xray','baritone','seedcrackerx']
fiw['resource_packs']['log']=False # Do not retain unrelated player pack lists.
fiw['profiling']['max_history']=30
files[configs/'fiw-mods-api/config.json']=json.dumps(fiw,ensure_ascii=False,indent=2)+'\n'
for file,data in files.items():
    file.parent.mkdir(parents=True,exist_ok=True)
    if file.exists():shutil.copyfile(file,file.with_name(file.name+'.before27'))
    file.write_text(data,encoding='utf-8')
print(json.dumps({'server':str(destination),'verified':3 if args.experimental_grim else 2,'grim':'experimental only' if args.experimental_grim else 'excluded: incompatible with modded inventories','antixray':'mode1','fiw':'blocked cheats, graphics allowed'}))
