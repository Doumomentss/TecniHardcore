"""Compile the 1.20.1 client overlay against the installed intermediary JAR.
No runtime remapping or Gradle download is needed; production Fabric uses these names.
"""
from pathlib import Path
import os, subprocess, json, zipfile, shutil

root = Path(__file__).resolve().parents[1]
project = root / 'custom_mods/death-overlay'
classes = project / 'build/classes'
classes.mkdir(parents=True, exist_ok=True)
mc = root / 'client/.fabric/remappedJars/minecraft-1.20.1-0.19.5/client-intermediary.jar'
classpath = os.pathsep.join(str(p) for p in [mc, *sorted((root/'client/libraries').rglob('*.jar')), *sorted((root/'client/.fabric/processedMods').glob('*.jar'))])
args = ['--release', '17', '-proc:none', '-encoding', 'UTF-8', '-classpath', classpath,
        '-d', str(classes), *map(str, sorted((project/'src').rglob('*.java')))]
argfile = project / 'build/javac.args'
argfile.write_text('\n'.join('"' + a.replace('\\', '/') + '"' for a in args), encoding='utf-8')
subprocess.run(['javac', '@' + str(argfile)], check=True)
manifest = {'schemaVersion':1, 'id':'tecni_death', 'version':'1.0.1',
            'name':'TecniHardcore Death Overlay', 'environment':'client',
            'entrypoints':{'client':['tecni.death.DeathOverlay']},
            'mixins':['tecni-death.mixins.json'],
            'depends':{'fabricloader':'>=0.14.0', 'fabric-command-api-v2':'*', 'minecraft':'1.20.1', 'java':'>=17'}}
mixins = {'required':True, 'package':'tecni.death.mixin', 'compatibilityLevel':'JAVA_17',
          'client':['DeathPacketMixin','RenderMixin'], 'injectors':{'defaultRequire':1}}
out = project/'build/tecni-death-overlay-1.0.0.jar'
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as jar:
    jar.writestr('fabric.mod.json', json.dumps(manifest))
    jar.writestr('tecni-death.mixins.json', json.dumps(mixins))
    jar.writestr('assets/tecni_death/sounds.json', json.dumps({'break':{'sounds':[{'name':'tecni_death/break','stream':True}]}}).replace('tecni_death/break', 'tecni_death:break'))
    jar.write(root/'assets/video_sin_fondo_transparente.fma', 'assets/tecni_death/death.fma')
    jar.write(root/'assets/video_sin_fondo_transparente.ogg', 'assets/tecni_death/sounds/break.ogg')
    for name in ('tecni/death/DeathOverlay.class', 'tecni/death/mixin/DeathPacketMixin.class', 'tecni/death/mixin/RenderMixin.class'):
        jar.write(classes/name, name)
for dest in ('client/mods', 'installer_payload/mods'):
    shutil.copy2(out, root/dest/out.name)
print(f'Built and installed {out.name} ({out.stat().st_size:,} bytes)')
