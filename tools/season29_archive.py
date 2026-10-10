"""Immutable pre-2.9 archive and hash-verified restore rehearsal.

Run while production server is stopped. The archive is outside BackupService's
server/backups rotation; it includes account data but must never be published.
"""
import hashlib, json, os, shutil, sys, tempfile, zipfile
from pathlib import Path

workspace=Path(__file__).resolve().parents[1]
root=workspace/'server'
archive_root=workspace/'season-archives'
archive_root.mkdir(exist_ok=True)
archive=archive_root/'season-2026-10-09-pre29.zip'
sources=['world','EasyAuth','config','recordings','ops.json','usercache.json','usernamecache.json','whitelist.json','server.properties','player_lives.json']
if archive.exists(): raise SystemExit('Archive already exists; refusing to overwrite')
files=[]
for relative in sources:
    path=root/relative
    if not path.exists(): continue
    candidates=path.rglob('*') if path.is_dir() else [path]
    for candidate in candidates:
        if candidate.is_symlink(): raise SystemExit(f'Link in archive source: {candidate}')
        if candidate.is_file(): files.append(candidate)
files.sort()
entries={}
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_STORED,allowZip64=True) as z:
    for path in files:
        relative=path.relative_to(root).as_posix()
        digest=hashlib.sha256()
        with path.open('rb') as src, z.open(relative,'w',force_zip64=True) as dst:
            while chunk:=src.read(1024*1024): digest.update(chunk);dst.write(chunk)
        entries[relative]={'sha256':digest.hexdigest(),'size':path.stat().st_size}
manifest={'archive':archive.name,'files':entries,'sha256':hashlib.file_digest(archive.open('rb'),'sha256').hexdigest()}
(archive_root/'season-2026-10-09-pre29.sha256.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
# A full restore on an isolated directory proves readability and exact file data.
restore=workspace/'tools'/'test-runtime'/'season29-restore-rehearsal'
if restore.exists(): raise SystemExit('Restore rehearsal destination already exists')
restore.mkdir(parents=True)
with zipfile.ZipFile(archive) as z:
    for info in z.infolist():
        relative=Path(info.filename)
        if relative.is_absolute() or '..' in relative.parts: raise SystemExit('Unsafe ZIP entry')
        target=restore/relative;target.parent.mkdir(parents=True,exist_ok=True)
        with z.open(info) as src,target.open('wb') as dst: shutil.copyfileobj(src,dst,1024*1024)
for relative,expected in entries.items():
    path=restore/relative
    with path.open('rb') as f: actual=hashlib.file_digest(f,'sha256').hexdigest()
    if actual!=expected['sha256']: raise SystemExit(f'Restore hash mismatch: {relative}')
(archive_root/'season-2026-10-09-pre29.restore-ok').write_text(f'{len(entries)} files verified\n',encoding='utf-8')
print(json.dumps({'archive':str(archive),'sha256':manifest['sha256'],'files':len(entries),'bytes':archive.stat().st_size,'restore':str(restore)}))
