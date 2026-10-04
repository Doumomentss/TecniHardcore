"""Read-only proof against the stopped-world backup; never mutates player records."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parents[1];S=R/'server';OUT=R/'tools/test-runtime/social27'
backup=json.loads((OUT/'backup-restoration-result.json').read_text());old=Path(backup['restoration'])
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before=json.loads((old/'tecnihardcore-souls.json').read_text());after=json.loads((S/'world/tecnihardcore-souls.json').read_text())
assert before==after,'Soul data changed'
players=list((old/'playerdata').glob('*.dat'))
for p in players:assert digest(S/'world/playerdata'/p.name)==digest(p),p.name
core=R/'installer_payload/mods/tecnihardcore-2.7.0.jar'
for p in [S/'mods/tecnihardcore-2.7.0.jar',R/'client/mods/tecnihardcore-2.7.0.jar']:assert digest(p)==digest(core),p
assert not (S/'mods/tecnihardcore-2.6.3.jar').exists()
assert not any((S/'mods').glob('grimac-*.jar'))
assert 'label = ENTRAR AL SERVIDOR' in (R/'client/config/fancymenu/customization/title_screen_layout.txt').read_text(encoding='utf8')
for name in ['ops.json','server.properties']:
    import zipfile
    with zipfile.ZipFile(backup['backup']) as archive:
        original=archive.read(name)
        if name=='server.properties':
            # Minecraft rewrites its timestamp comment at every normal startup.
            properties=lambda text:dict(line.split('=',1) for line in text.splitlines() if line and not line.startswith('#') and '=' in line)
            assert properties(original.decode('utf8'))==properties((S/name).read_text(encoding='utf8')),name
        else:assert hashlib.sha256(original).hexdigest()==digest(S/name),name
npcs=json.loads((S/'config/tecnihardcore/npcs.json').read_text(encoding='utf8'))
assert set(npcs)=={'ines','bruno','selma'}
assert all(n['y']==96 for n in npcs.values())
result={'passed':True,'version':'2.7.0','playerFilesUnchanged':len(players),'soulRecordsUnchanged':len(after['players']),'coreSha256':digest(core),'npcNames':[n['name'] for n in npcs.values()],'npcsAtSpawn':True,'customButtonPreserved':True,'authAndOperatorPropertiesPreserved':True,'grimExcluded':True}
(OUT/'production-preservation-result.json').write_text(json.dumps(result,indent=2,ensure_ascii=False),encoding='utf8')
print('PASS production: lives, UUIDs and player files unchanged; 3 NPCs, final hashes and custom button verified.')
