"""Latest-world preservation fixture; no inherited edits from earlier QA worlds."""
from pathlib import Path
import shutil,json,nbtlib
R=Path(__file__).resolve().parents[1];B=R/'tools/test-runtime/expansion25';S=B/'preservation25'
assert not S.exists(),'Use a fresh preservation fixture'
S.mkdir();source=B/'server'
for name in ['fabric-server-launch.jar','fabric-server-launcher.properties','server.jar','eula.txt']:shutil.copy2(source/name,S/name)
for name in ['libraries','versions','mods','config']:shutil.copytree(source/name,S/name)
restore=Path(json.loads((B/'backup-restoration-result.json').read_text())['restoration']);shutil.copytree(restore,S/'world')
n=nbtlib.load(S/'world/level.dat');n['Data']['GameRules']['randomTickSpeed']=nbtlib.String('0');n['Data']['GameRules']['doMobSpawning']=nbtlib.String('false');n.save()
(S/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25569\nlevel-name=world\nonline-mode=false\ndifficulty=hard\nview-distance=11\nsimulation-distance=8\nmax-players=3\nspawn-protection=0\nenable-rcon=false\nallow-flight=true\n')
(S/'.tecni-test-world').write_text('Fresh latest-world copy, no production player tests.\n');(S/'ops.json').write_text('[]')
voice=S/'config/voicechat/voicechat-server.properties';voice.parent.mkdir(parents=True,exist_ok=True);voice.write_text('port=24569\n')
print(S)
