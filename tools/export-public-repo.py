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
copy('PROBAR_JEFE.bat');copy('README-PUBLIC.md','README.md');copy('dist/THIRD-PARTY-MODS.json','THIRD-PARTY-MODS.json')
for src,dst in [('sanctuary-target-cinema-start.png','ritual-cupula.png'),('sanctuary-target-descent.png','ritual-descenso.png')]:
    picture=root/'tools/test-runtime/visuals22'/src
    if picture.exists():copy(picture.relative_to(root),'docs/'+dst)
for src,dst in [('tools/test-runtime/boss-preview/game/screenshots/sanctuary-board-complete.png','docs/tablon-reliquias.png'),('tools/test-runtime/boss-preview/game/screenshots/sanctuary-phase-3-transform-complete.png','docs/custodio-prueba.png'),('tools/test-runtime/dashboard23/dashboard.png','docs/launcher-servidor.png')]:
    if (root/src).exists():copy(src,dst)
copy('dist/update.json','release/update.json')
if (root/'tools/test-runtime/update241/waiting-for-pak.png').exists():copy('tools/test-runtime/update241/waiting-for-pak.png','docs/actualizacion-espera.png')
copy('tools/test-update241.cs')
for name in ['spawn24-final','boss-death-fracture','boss-death-dissipation','content24-inventory']:
    src=f'tools/test-runtime/boss-preview/game/screenshots/sanctuary-{name}.png'
    if (root/src).exists():copy(src,f'docs/{name}.png')
version=json.loads((root/'launcher/package.json').read_text())['version']
copy(f'dist/manifest-{version}.json',f'release/manifest-{version}.json');copy(f'dist/SHA256SUMS-{version}.txt',f'release/SHA256SUMS-{version}.txt')
(target/'.gitignore').write_text('node_modules/\n.gradle/\n**/build/\n**/pack/\npublish/\ndist/\nbackups/\nlogs/\nserver/\nclient/\nplayit/\ninstaller_payload/mods/\nTecniHardcore Launcher/\ntools/test-runtime/\ntools/build-launcher-stage/\ntools/vendor/\n*.fma\ninstaller/*.zip\nconnection.local.json\n*.sqlite*\n*.db\n.env*\n')
(target/'.gitattributes').write_text('* text=auto\n*.png binary\n*.jpg binary\n*.gif binary\n*.ogg binary\n*.webm binary\n*.dat binary\n')
files=[p for p in target.rglob('*') if p.is_file() and '.git' not in p.relative_to(target).parts]
assert all(p.stat().st_size<100*1024**2 for p in files),'Oversize Git file'
print('Public source export:',target,len(files),'files;',sum(p.stat().st_size for p in files)//1048576,'MiB')
