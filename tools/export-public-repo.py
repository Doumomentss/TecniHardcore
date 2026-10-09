"""Copy an explicit source allowlist. Never export the live server or authentication data."""
from pathlib import Path
import shutil,json
root=Path(__file__).resolve().parents[1];target=root/'publish/TecniHardcore';target.mkdir(parents=True,exist_ok=True)
def copy(relative,destination=None):
    src=root/relative;dst=target/(destination or relative);dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst)
for directory in ['launcher','custom_mods/hardcore/src','custom_mods/death-overlay/src','installer','installer_payload/config','installer_payload/resourcepacks','tools/spawn-pack','tools/spawn24-pack']:
    for src in (root/directory).rglob('*'):
        if not src.is_file():continue
        relative=src.relative_to(root)
        if any(part in {'node_modules','pack','build','.gradle'} for part in relative.parts) or src.suffix in {'.zip','.fma'}:continue
        copy(relative)
for name in ['build.gradle','settings.gradle','gradle.properties']:copy('custom_mods/hardcore/'+name)
copy('custom_mods/death-overlay/README.md')
for name in ['mods-downloads.json','shaders-download.json','servers.dat']:copy('installer_payload/'+name)
for name in ['video_sin_fondo_transparente.webm','video_sin_fondo_transparente.ogg','revive_heart_crystal.png']:copy('assets/'+name)
for name in ['package-launcher.ps1','build-installer.py','create-release-manifest.py','build-sanctuary-assets.py','build-death-video.py','build-death-mod.py','fetch-development.cjs','development-dependencies.json','prepare-payload.cjs','export-public-repo.py','publish-release.ps1','test-installer-transactions.py','test-installer-transactions.cs','test-installation.cjs','verify-update-install.cjs','test-launcher-update-ui.cjs']:copy('tools/'+name)
for name in ['ADMINISTRACION.md','RESULTADOS-2.1.md','RESULTADOS-2.2.md','RESULTADOS-ACTUALIZADOR.md','SPAWN.md','PRUEBA-JEFE.md','RESULTADOS-2.3.md','RESULTADOS-2.4.md','RESULTADOS-2.4.1.md','SEGURIDAD-INSTALADOR.md']:copy(name)
for name in ['build-spawn.py','find-sanctuary-site.py','build-trial-assets.py','preview-boss.cjs','test-dashboard.cjs','test-dashboard-ui.cjs','verify-release.cjs','build-espectro-assets.py','add-graphics24.py','test-launcher24.cjs','add-content24.py','content24-lock.json','improve-boss24-assets.py','build-spawn24.py','preview-spawn24.cjs','qa-content24.cjs']:copy('tools/'+name)
for name in ['build-expansion25-assets.py','add-content25.py','content25-lock.json','stage-expansion25.py','start-expansion25.ps1','backup-before25.py','preview-expansion25.cjs','qa-expansion25.cjs','qa-native-mounts25.py','qa-disconnect25.cjs','qa-weather25.cjs','qa-shaders25.py','qa-shader-actors25.cjs','qa-event-mail25.cjs','qa-boss-loot25.cjs','qa-restart25.cjs','qa-storm-events25.cjs','qa-mount-safety25.cjs','qa-worldgen25.cjs','qa-preservation25.py','verify-world25.py','test-pack-handshake.cjs']:copy('tools/'+name)
for name in ['EVENTOS-Y-MONTURAS.md','RESULTADOS-2.5.md']:copy(name)

for name in ['NOVEDADES-2.6.md','RESULTADOS-2.6.md','NOVEDADES-2.6.1.md','RESULTADOS-2.6.1.md']:copy(name)
for name in ['qa-event-commands251.cjs','qa-weather26.cjs','qa-native-weather26.cjs','qa-native-final26.cjs','build-weather26-audio.py','qa-native-beacon26.cjs','backup-before26.py']:copy('tools/'+name)
for name in ['qa-weather261.cjs','qa-native-weather261.cjs','qa-supplement-weather261.cjs','build-weather261-audio.py','backup-before261.py','verify-production261.py']:copy('tools/'+name)
for name in ['NOVEDADES-2.6.2.md','RESULTADOS-2.6.2.md']:copy(name)
for name in ['qa-tornado262.cjs','qa-tornado-pull262.cjs','verify-kayra262.py','backup-before262.py','verify-production262.py']:copy('tools/'+name)
copy('tools/test-runtime/expansion25/game/screenshots/sanctuary-t262-kayra-storm.png','docs/t262-kayra-storm.png')
for name in ['NOVEDADES-2.6.3.md','RESULTADOS-2.6.3.md']:copy(name)
for name in ['NOVEDADES-2.7.md','RESULTADOS-2.7.md','NPCS-EQUIPOS-MERCADO.md']:copy(name)
for name in ['prepare-security27.py','security27-lock.json','preview-social27.cjs','test-social27.cjs','test-security27.cjs','test-social27-recovery.py','test-guard27.cjs','qa-civic27-native.cjs','qa-plaza-anchor27.cjs','backup-before27.py','verify-production27.py']:copy('tools/'+name)
for src in (root/'tools/security27-configs').glob('*'):
    if src.is_file():copy(src.relative_to(root))
for name in ['civic27-dialogue','civic27-market-scale2','civic27-market-scale4-next','civic27-market-fullscreen','civic27-spawn-npc','civic27-custom-skin-final']:
    src=f'tools/test-runtime/expansion25/game/screenshots/sanctuary-{name}.png'
    if (root/src).exists():copy(src,f'docs/{name}.png')
for name in ['qa-rubble263.cjs','backup-before263.py','verify-production263.py']:copy('tools/'+name)
for name in ['mass-uproot','dense-vortex','stopped']:copy(f'tools/test-runtime/expansion25/game/screenshots/sanctuary-r263-{name}.png',f'docs/r263-{name}.png')
copy('playit/start-playit.ps1');copy('server/start-server.ps1');copy('server/iniciar.bat');copy('INICIAR_SERVIDOR.bat');copy('INICIAR_PLAYIT.bat')
copy('tools/server-control/GracefulStop.java');copy('tools/server-control/MANIFEST.MF')
for name in ['w26-icons-final','w26-tornado','w26-electric-ultra-quality','w26-beacon-99000-six-chunks','w26-skin-full-final']:
    src=f'tools/test-runtime/expansion25/game/screenshots/sanctuary-{name}.png'
    if (root/src).exists():copy(src,f'docs/{name}.png')
for name in ['tornado-slope','meteor-flight','quake-fracture']:
    src=f'tools/test-runtime/expansion25/game/screenshots/sanctuary-w261-{name}.png'
    if (root/src).exists():copy(src,f'docs/w261-{name}.png')
copy('assets/icons26/PROMPTS.md','docs/iconos-2.6-prompts.md')
copy('PROBAR_JEFE.bat');copy('README-PUBLIC.md','README.md');copy('dist/THIRD-PARTY-MODS.json','THIRD-PARTY-MODS.json')
for src,dst in [('sanctuary-target-cinema-start.png','ritual-cupula.png'),('sanctuary-target-descent.png','ritual-descenso.png')]:
    picture=root/'tools/test-runtime/visuals22'/src
    if picture.exists():copy(picture.relative_to(root),'docs/'+dst)
for src,dst in [('tools/test-runtime/boss-preview/game/screenshots/sanctuary-board-complete.png','docs/tablon-reliquias.png'),('tools/test-runtime/boss-preview/game/screenshots/sanctuary-phase-3-transform-complete.png','docs/custodio-prueba.png'),('tools/test-runtime/dashboard23/dashboard.png','docs/launcher-servidor.png')]:
    if (root/src).exists():copy(src,dst)
copy('dist/update.json','release/update.json')
for name in ['tornado-ultra-quality-fixed','meteoritos-ultra-quality-fixed','mount-3-ground','mount-5-flight','event-circuito-hardcore','event-panel-final']:
    src=f'tools/test-runtime/expansion25/game/screenshots/sanctuary-{name}.png'
    if (root/src).exists():copy(src,f'docs/{name}.png')
if (root/'tools/test-runtime/update241/waiting-for-pak.png').exists():copy('tools/test-runtime/update241/waiting-for-pak.png','docs/actualizacion-espera.png')
copy('tools/test-update241.cs')
for name in ['spawn24-final','boss-death-fracture','boss-death-dissipation','content24-inventory']:
    src=f'tools/test-runtime/boss-preview/game/screenshots/sanctuary-{name}.png'
    if (root/src).exists():copy(src,f'docs/{name}.png')
for name in ['NOVEDADES-2.7.1.md','RESULTADOS-2.7.1.md']:copy(name)
for name in ['backup-before271.py','qa-five-lives271.cjs','build-npc-skins.py','install-npc-skins.py','test-npc-skins27.cjs']:copy('tools/'+name)
for name in ['ines','bruno','selma','preview']:copy('assets/npc-skins/'+name+'.png')
copy('tools/test-runtime/expansion25/game/screenshots/sanctuary-five-lives271-scale2.png','docs/five-lives271.png')
version=json.loads((root/'launcher/package.json').read_text())['version']
copy(f'dist/manifest-{version}.json',f'release/manifest-{version}.json');copy(f'dist/SHA256SUMS-{version}.txt',f'release/SHA256SUMS-{version}.txt')
(target/'.gitignore').write_text('node_modules/\n.gradle/\n**/build/\n**/pack/\npublish/\ndist/\nbackups/\nlogs/\nserver/\nclient/\nplayit/\ninstaller_payload/mods/\nTecniHardcore Launcher/\ntools/test-runtime/\ntools/build-launcher-stage/\ntools/vendor/\n*.fma\ninstaller/*.zip\nconnection.local.json\n*.sqlite*\n*.db\n.env*\n')
(target/'.gitattributes').write_text('* text=auto\n*.png binary\n*.jpg binary\n*.gif binary\n*.ogg binary\n*.webm binary\n*.dat binary\n')
files=[p for p in target.rglob('*') if p.is_file() and '.git' not in p.relative_to(target).parts]
assert all(p.stat().st_size<100*1024**2 for p in files),'Oversize Git file'
print('Public source export:',target,len(files),'files;',sum(p.stat().st_size for p in files)//1048576,'MiB')
