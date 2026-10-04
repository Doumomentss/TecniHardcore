from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[1]
version=json.loads((root/'launcher/package.json').read_text())['version']
pack=json.loads((root/'installer_payload/config/tecnihardcore/connection.json').read_text())['packVersion']
setup=root/f'dist/TecniHardcore-Setup-{version}.exe'
credits=root/'dist/THIRD-PARTY-MODS.json'
if not credits.exists():credits.write_bytes((root/'THIRD-PARTY-MODS.json').read_bytes())
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
protocol=8 if tuple(map(int,pack.split("."))) >= (2,6,1) else 7 if tuple(map(int,pack.split('.'))) >= (2,6,0) else 6 if tuple(map(int,pack.split('.'))) >= (2,5,0) else 5
notes='Cataclismos administrativos sin destrucción, cinco monturas de cristal y botín personal del Custodio; eventos en ensayo o hardcore, biomas y decoración. Conserva identidad portable, gráficos, conexión directa y el parche del actualizador 2.4.1.' if protocol==6 else 'Corrección de actualización: descargas independientes, instalación visible, espera de archivos bloqueados y verificación antes de reabrir. Conserva Minecraft, nombre, carpeta y preferencias; paquete de juego 2.4.0.' if version=='2.4.1' else 'Más armas, armaduras y enemigos; Custodio con ataques y derrotas renovadas, altar ampliado y plaza TECNIHARDCORE. Incluye baliza de auxilio, identidad portable y cinco perfiles gráficos.'
manifest={'schema':1,'version':version,'packVersion':pack,'minecraft':'1.20.1','ritualProtocol':3,'soulProtocol':3,'packProtocol':protocol,'notes':notes,'installer':{'url':f'https://github.com/Doumomentss/TecniHardcore/releases/download/v{version}/{setup.name}','bytes':setup.stat().st_size,'sha256':sha(setup)}}
if protocol>=7:manifest['notes']='Tormentas con alarma y audio real, tornado de ancho configurable y escombros visuales, destrucción opcional por niveles, baliza de largo alcance, skins, iconos originales y corrección de Espacio. Eventos sin ID obligatorio; conserva mundo, identidades y actualización segura.'
if protocol==8:manifest['notes']='Tornado que sigue el terreno y baja pendientes, admite obstáculos y se mueve un 50 % más rápido; viento sin lluvia metálica. Meteoritos visibles con daño real y cráteres 0–4; terremoto con fracturas profundas. Conserva datos y protección del spawn.'
(root/'dist/update.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
core=root/f'installer_payload/mods/tecnihardcore-{pack}.jar'
compiled=root/f'custom_mods/hardcore/build/libs/tecnihardcore-{pack}.jar'
if compiled.exists():assert sha(core)==sha(compiled),'Copy the newly compiled mod into installer_payload/mods before publishing'
files=[setup,core,root/'installer_payload/mods/tecni-death-overlay-1.0.0.jar',root/'dist/update.json',credits]
(root/f'dist/SHA256SUMS-{version}.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in files))
full={'launcherVersion':version,'packVersion':pack,'minecraft':'1.20.1','ritualProtocol':3,'soulProtocol':3,'packProtocol':protocol,'fabricLoader':'0.19.5','geckolib':'4.8.4','files':{p.name:{'bytes':p.stat().st_size,'sha256':sha(p)} for p in files}}
(root/f'dist/manifest-{version}.json').write_text(json.dumps(full,indent=2)+'\n')
print('Release manifest:',version,'pack',pack,'installer',setup.stat().st_size//1048576,'MiB')
