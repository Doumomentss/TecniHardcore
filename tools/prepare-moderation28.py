"""Pinned server-only recorder dependencies; never adds them to player packs."""
from pathlib import Path
import argparse,json,hashlib,urllib.request,socket,zipfile,io,subprocess
R=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--server',type=Path,default=R/'server');a=p.parse_args();S=a.server.resolve()
assert S.is_relative_to(R) and (S/'fabric-server-launch.jar').is_file()
if S!=R/'server':assert (S/'.tecni-test-world').is_file()
properties=dict(line.split('=',1) for line in (S/'server.properties').read_text(encoding='utf8').splitlines() if '=' in line and not line.startswith('#'))
with socket.socket() as connection:
 connection.settimeout(1)
 if connection.connect_ex(('127.0.0.1',int(properties['server-port'])))==0:raise SystemExit('Stop the target server before installing recorder dependencies.')
lock=json.loads((R/'tools/moderation28-lock.json').read_text())
if S==R/'server':
 java=lock['serverJava']; archive=R/'tools/vendor'/java['name']
 data=archive.read_bytes() if archive.exists() else urllib.request.urlopen(java['url']).read()
 assert len(data)==java['size'] and hashlib.sha256(data).hexdigest()==java['sha256']
 archive.write_bytes(data);runtime=S/'runtime';runtime.mkdir(exist_ok=True)
 with zipfile.ZipFile(io.BytesIO(data)) as z:
  for name in z.namelist():
   assert not Path(name).is_absolute() and '..' not in Path(name).parts
   assert (runtime/name).resolve().is_relative_to(runtime.resolve())
  z.extractall(runtime)
 executable=next(runtime.glob('jdk-21*/bin/java.exe'))
 check=subprocess.run([str(executable),'-version'],capture_output=True,text=True,check=True)
 assert 'version "21.' in check.stderr
 (S/'runtime-java.json').write_text(json.dumps({'java':executable.relative_to(S).as_posix(),'major':21,'archiveSha256':java['sha256']},indent=2))
for f in lock['files']:
 vendor=R/'tools/vendor'/f['name']
 data=vendor.read_bytes() if vendor.exists() else urllib.request.urlopen(f['url']).read()
 assert len(data)==f['size'] and hashlib.sha256(data).hexdigest()==f['sha256'] and hashlib.sha512(data).hexdigest()==f['sha512']
 (S/'mods'/f['name']).write_bytes(data)
config=S/'config/ServerReplay/config.json';config.parent.mkdir(parents=True,exist_ok=True)
settings={'enabled':True,'include_resource_packs':False,'allow_downloading_replays':False,'player_recording_name':'{uuid}','world_name':'TecniHardcore','server_name':'TecniHardcore','player_recording_path':'recordings/tecni-moderacion','chunk_recording_path':'recordings/tecni-chunks','max_file_size':'512MB','restart_after_max_file_size':False,'max_duration':'0s','restart_after_max_duration':False,'recover_unsaved_replays':True,'include_compressed_in_status':False,'notify_admins_of_status':False,'ignore_sound_packets':False,'ignore_light_packets':False,'ignore_chat_packets':True,'ignore_scoreboard_packets':False,'record_voice_chat':False,'player_predicate':{'type':'none'},'chunks':[]}
if config.exists():config.with_suffix('.before-tecni28.json').write_bytes(config.read_bytes())
config.write_text(json.dumps(settings,indent=2),encoding='utf8')
print('Verified server-only ServerReplay and Kotlin; private player recording, no chat/voice/HTTP downloads.')
