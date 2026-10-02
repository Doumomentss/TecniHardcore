from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[1]
version=json.loads((root/'launcher/package.json').read_text())['version']
pack=json.loads((root/'installer_payload/config/tecnihardcore/connection.json').read_text())['packVersion']
setup=root/f'dist/TecniHardcore-Setup-{version}.exe'
credits=root/'dist/THIRD-PARTY-MODS.json'
if not credits.exists():credits.write_bytes((root/'THIRD-PARTY-MODS.json').read_bytes())
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
manifest={'schema':1,'version':version,'packVersion':pack,'minecraft':'1.20.1','ritualProtocol':3,'notes':'Núcleo monumental, cúpula oscura de 32 bloques y resurrección cinematográfica con la skin y aura azul. Conserva tus ajustes y el botón de conexión.','installer':{'url':f'https://github.com/Doumomentss/TecniHardcore/releases/download/v{version}/{setup.name}','bytes':setup.stat().st_size,'sha256':sha(setup)}}
(root/'dist/update.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
core=root/f'installer_payload/mods/tecnihardcore-{pack}.jar'
compiled=root/f'custom_mods/hardcore/build/libs/tecnihardcore-{pack}.jar'
if compiled.exists():assert sha(core)==sha(compiled),'Copy the newly compiled mod into installer_payload/mods before publishing'
files=[setup,core,root/'installer_payload/mods/tecni-death-overlay-1.0.0.jar',root/'dist/update.json',credits]
(root/f'dist/SHA256SUMS-{version}.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in files))
full={'launcherVersion':version,'packVersion':pack,'minecraft':'1.20.1','ritualProtocol':3,'fabricLoader':'0.19.5','geckolib':'4.8.4','files':{p.name:{'bytes':p.stat().st_size,'sha256':sha(p)} for p in files}}
(root/f'dist/manifest-{version}.json').write_text(json.dumps(full,indent=2)+'\n')
print('Release manifest:',version,'pack',pack,'installer',setup.stat().st_size//1048576,'MiB')
