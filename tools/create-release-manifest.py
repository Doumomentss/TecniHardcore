from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[1]
version=json.loads((root/'launcher/package.json').read_text())['version']
pack=json.loads((root/'installer_payload/config/tecnihardcore/connection.json').read_text())['packVersion']
setup=root/f'dist/TecniHardcore-Setup-{version}.exe'
credits=root/'dist/THIRD-PARTY-MODS.json'
if not credits.exists():credits.write_bytes((root/'THIRD-PARTY-MODS.json').read_bytes())
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
protocol=13 if tuple(map(int,pack.split("."))) >= (2,8,0) else 12 if tuple(map(int,pack.split("."))) >= (2,7,1) else 11 if tuple(map(int,pack.split("."))) >= (2,7,0) else 10 if tuple(map(int,pack.split("."))) >= (2,6,3) else 9 if tuple(map(int,pack.split("."))) >= (2,6,2) else 8 if tuple(map(int,pack.split("."))) >= (2,6,1) else 7 if tuple(map(int,pack.split('.'))) >= (2,6,0) else 6 if tuple(map(int,pack.split('.'))) >= (2,5,0) else 5
notes='Cataclismos administrativos sin destrucciÃ³n, cinco monturas de cristal y botÃ­n personal del Custodio; eventos en ensayo o hardcore, biomas y decoraciÃ³n. Conserva identidad portable, grÃ¡ficos, conexiÃ³n directa y el parche del actualizador 2.4.1.' if protocol==6 else 'CorrecciÃ³n de actualizaciÃ³n: descargas independientes, instalaciÃ³n visible, espera de archivos bloqueados y verificaciÃ³n antes de reabrir. Conserva Minecraft, nombre, carpeta y preferencias; paquete de juego 2.4.0.' if version=='2.4.1' else 'MÃ¡s armas, armaduras y enemigos; Custodio con ataques y derrotas renovadas, altar ampliado y plaza TECNIHARDCORE. Incluye baliza de auxilio, identidad portable y cinco perfiles grÃ¡ficos.'
manifest={'schema':1,'version':version,'packVersion':pack,'minecraft':'1.20.1','ritualProtocol':3,'soulProtocol':3,'packProtocol':protocol,'notes':notes,'installer':{'url':f'https://github.com/Doumomentss/TecniHardcore/releases/download/v{version}/{setup.name}','bytes':setup.stat().st_size,'sha256':sha(setup)}}
if protocol>=7:manifest['notes']='Tormentas con alarma y audio real, tornado de ancho configurable y escombros visuales, destrucciÃ³n opcional por niveles, baliza de largo alcance, skins, iconos originales y correcciÃ³n de Espacio. Eventos sin ID obligatorio; conserva mundo, identidades y actualizaciÃ³n segura.'
if protocol==8:manifest['notes']='Tornado que sigue el terreno y baja pendientes, admite obstÃ¡culos y se mueve un 50 % mÃ¡s rÃ¡pido; viento sin lluvia metÃ¡lica. Meteoritos visibles con daÃ±o real y crÃ¡teres 0â€“4; terremoto con fracturas profundas. Conserva datos y protecciÃ³n del spawn.'
if protocol==9:manifest['notes']='Tornado con destrucciÃ³n 0â€“20, demoliciÃ³n de columnas completas y fuerza proporcional al radio y la anchura. Conserva meteoritos, terremotos, mundo, identidades y protecciÃ³n del spawn.'
if protocol==10:manifest['notes']='Tornado con arranque masivo de bloques y escombros reales sincronizados. Niveles 16â€“20 destruyen bloques duros y contenedores; mayor densidad exterior y demoliciÃ³n mÃ¡s rÃ¡pida. Conserva datos y protecciÃ³n del spawn.'
if protocol==11:manifest['notes']='NPCs con diálogos y skins, misiones iniciales, equipos y chat privado, treguas de conexión y mercado con Cristales Tecni. AntiXray y filtros de cliente; conserva mundo, vidas y actualización segura.'
if protocol==12:manifest['notes']='Cinco vidas, cinco cristales en el HUD y launcher, migración única que conserva eliminados y muertes previas. Mantiene skins originales, equipos, mercado y actualización segura.'
if protocol==13:manifest['notes']='Replays automáticos de muertes para moderación y protección PvP mutua de 30 minutos de conexión, persistente y con HUD. Mantiene cinco vidas, inventarios, equipos, mercado y skins.'
if tuple(map(int,version.split('.'))) >= (2,8,1):manifest['notes']='Corrige grabación y reproducción de muertes con Simple Voice Chat y AntiXray. Añade /tecni replay lista Nombre y play Nombre ID, con IDs por jugador y hasta 30 segundos previos. Autentica bots Carpet de prueba, simplifica el launcher y conserva vidas, inventarios y preferencias.'
(root/'dist/update.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
core=root/f'installer_payload/mods/tecnihardcore-{pack}.jar'
compiled=root/f'custom_mods/hardcore/build/libs/tecnihardcore-{pack}.jar'
if compiled.exists():assert sha(core)==sha(compiled),'Copy the newly compiled mod into installer_payload/mods before publishing'
files=[setup,core,root/'installer_payload/mods/tecni-death-overlay-1.0.0.jar',root/'dist/update.json',credits]
(root/f'dist/SHA256SUMS-{version}.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in files))
full={'launcherVersion':version,'packVersion':pack,'minecraft':'1.20.1','ritualProtocol':3,'soulProtocol':3,'packProtocol':protocol,'fabricLoader':'0.19.5','geckolib':'4.8.4','files':{p.name:{'bytes':p.stat().st_size,'sha256':sha(p)} for p in files}}
(root/f'dist/manifest-{version}.json').write_text(json.dumps(full,indent=2)+'\n')
print('Release manifest:',version,'pack',pack,'installer',setup.stat().st_size//1048576,'MiB')
