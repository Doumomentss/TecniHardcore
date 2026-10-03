"""Restore a verified world backup and build an isolated, localhost-only 2.5 host."""
from pathlib import Path
import zipfile,shutil,json,hashlib
R=Path(__file__).resolve().parents[1];S=R/'tools/test-runtime/expansion25/server';S.mkdir(parents=True,exist_ok=True)
if not (S/'world').exists():
 backups=sorted((R/'server/backups/tecnihardcore').glob('world-*.zip'));backup=backups[-1]
 with zipfile.ZipFile(backup) as z:
  assert z.testzip() is None
  for info in z.infolist():
   relative=Path(info.filename)
   assert not relative.is_absolute() and '..' not in relative.parts
   target=S/'world'/relative
   if info.is_dir():target.mkdir(parents=True,exist_ok=True)
   else:target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(info))
 print('Restored:',backup.name)
for name in ['server.jar','fabric-server-launch.jar','fabric-server-launcher.properties','eula.txt']:shutil.copy2(R/'server'/name,S/name)
for directory in ['libraries','versions']:
 if not (S/directory).exists():shutil.copytree(R/'server'/directory,S/directory)
mods=S/'mods';mods.mkdir(exist_ok=True)
for p in (R/'client/mods').glob('*.jar'):
 if not p.name.startswith('tecnihardcore-'):shutil.copy2(p,mods/p.name)
for p in (R/'tools/test-runtime/expansion25/content').glob('*.jar'):shutil.copy2(p,mods/p.name)
for p in mods.glob('tecnihardcore-*.jar'):p.unlink()
shutil.copy2(R/'custom_mods/hardcore/build/libs/tecnihardcore-2.5.0.jar',mods/'tecnihardcore-2.5.0.jar')
(S/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25568\nlevel-name=world\nonline-mode=false\ndifficulty=hard\nview-distance=8\nsimulation-distance=6\nmax-players=8\nspawn-protection=0\nenable-rcon=false\nallow-flight=true\n',encoding='utf8')
# No EasyAuth database or Playit credentials are copied. Test flag is required to waive auth.
(S/'ops.json').write_text('[]\n',encoding='utf8')
print('Isolated pack:',len(list(mods.glob('*.jar'))),'jars')
print('Core:',hashlib.sha256((mods/'tecnihardcore-2.5.0.jar').read_bytes()).hexdigest())
(S/'.tecni-test-world').write_text('Isolated restored copy; production data must never be targeted.\n',encoding='utf8')

voice=S/"config/voicechat/voicechat-server.properties"
voice.parent.mkdir(parents=True,exist_ok=True)
voice.write_text("port=24568\n",encoding="utf8")
shutil.copy2(R/'installer_payload/config/lithium.properties',S/'config/lithium.properties')
