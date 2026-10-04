"""Consistent stopped-server backup, with an exact restoration rehearsal."""
from pathlib import Path
import hashlib, json, zipfile, socket, datetime
R=Path(__file__).resolve().parents[1]; S=R/'server'; OUT=R/'tools/test-runtime/tornado262'
try:
    with socket.create_connection(('127.0.0.1',25565),timeout=1): pass
except OSError: pass
else: raise RuntimeError('Stop production cleanly before backing up')
stamp=datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
backup=R/'backups'/f'before-tornado262-{stamp}.zip'
restore=OUT/f'restore-before262-{stamp}'
assert not backup.exists() and not restore.exists()
files=[]
for directory in ['world','mods','config','private','EasyAuth']:
    files.extend(p for p in (S/directory).rglob('*') if p.is_file())
for name in ['ops.json','server.properties','start-server.ps1','eula.txt']:
    if (S/name).exists(): files.append(S/name)
digest=lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
worldHashes={p.relative_to(S/'world').as_posix():digest(p) for p in files if p.is_relative_to(S/'world')}
backup.parent.mkdir(exist_ok=True); OUT.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(backup,'w',zipfile.ZIP_DEFLATED,compresslevel=1) as z:
    for p in files: z.write(p,p.relative_to(S).as_posix())
with zipfile.ZipFile(backup) as z:
    assert z.testzip() is None
    for relative in worldHashes:
        target=(restore/relative).resolve()
        assert target.is_relative_to(restore.resolve())
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_bytes(z.read('world/'+relative))
for relative,expected in worldHashes.items():
    assert digest(restore/relative)==expected,relative
    assert digest(S/'world'/relative)==expected,'Production changed during backup'
result={'backup':str(backup),'backupSha256':digest(backup),'restoration':str(restore),'worldFiles':len(worldHashes),'worldHashes':worldHashes,'crcVerified':True,'restoredExactly':True}
(OUT/'backup-restoration-result.json').write_text(json.dumps(result,indent=2),encoding='utf8')
print('Consistent backup:',backup.name)
print('PASS ZIP CRC and exact restoration:',len(worldHashes),'world files; original unchanged.')
