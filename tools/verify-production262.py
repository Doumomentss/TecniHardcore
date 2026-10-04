"""Read-only preservation proof against the stopped-server backup."""
from pathlib import Path
import hashlib, json
R=Path(__file__).resolve().parents[1]; OUT=R/'tools/test-runtime/tornado262'; S=R/'server'
backup=json.loads((OUT/'backup-restoration-result.json').read_text(encoding='utf8'))
old=Path(backup['restoration']); digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before=json.loads((old/'tecnihardcore-souls.json').read_text(encoding='utf8'))
after=json.loads((S/'world/tecnihardcore-souls.json').read_text(encoding='utf8'))
assert before==after,'Soul records changed during deployment'
players=list((old/'playerdata').glob('*.dat'))
for p in players: assert digest(S/'world/playerdata'/p.name)==digest(p),'Player data changed: '+p.name
core=R/'installer_payload/mods/tecnihardcore-2.6.2.jar'
for p in [S/'mods/tecnihardcore-2.6.2.jar',R/'client/mods/tecnihardcore-2.6.2.jar']:
    assert digest(p)==digest(core),'Core mismatch'
assert not (S/'mods/tecnihardcore-2.6.1.jar').exists()
assert not (R/'client/mods/tecnihardcore-2.6.1.jar').exists()
title=(R/'client/config/fancymenu/customization/title_screen_layout.txt').read_text(encoding='utf8')
assert 'label = ENTRAR AL SERVIDOR' in title
result={'version':'2.6.2','soulRecordsUnchanged':len(after['players']),'playerDataFilesUnchanged':len(players),'coreSha256':digest(core),'customButtonPreserved':True}
(OUT/'production-preservation-result.json').write_text(json.dumps(result,indent=2),encoding='utf8')
print('PASS: preserved',len(after['players']),'soul records and',len(players),'player files; final core hashes and button match.')
