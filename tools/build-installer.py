from pathlib import Path
import zipfile,hashlib,subprocess,json,re
root=Path(__file__).resolve().parents[1]
launcher=root/'TecniHardcore Launcher'
version=json.loads((root/'launcher/package.json').read_text())['version']
assert re.fullmatch(r'\d+\.\d+\.\d+',version)
(root/'installer/BuildInfo.cs').write_text('using System.Reflection;\n[assembly: AssemblyTitle("TecniHardcore Setup")]\n[assembly: AssemblyDescription("Instalador y actualizador de TecniHardcore, Minecraft 1.20.1 Fabric")]\n[assembly: AssemblyCompany("TecniHardcore")]\n[assembly: AssemblyProduct("TecniHardcore")]\n[assembly: AssemblyVersion("'+version+'.0")]\n[assembly: AssemblyFileVersion("'+version+'.0")]\nnamespace TecniHardcoreInstaller { static class BuildInfo { public const string Version="'+version+'"; } }\n',encoding='utf8')
files=[p for p in launcher.rglob('*') if p.is_file() and p.relative_to(launcher).parts[0] in {'locales','resources','chrome_100_percent.pak','chrome_200_percent.pak','d3dcompiler_47.dll','dxcompiler.dll','dxil.dll','ffmpeg.dll','icudtl.dat','LICENSE','LICENSES.chromium.html','resources.pak','snapshot_blob.bin','TecniHardcore Launcher.exe','v8_context_snapshot.bin','version','vk_swiftshader_icd.json','vk_swiftshader.dll','vulkan-1.dll'} and p.name!='debug.log']
manifest='\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.relative_to(launcher).as_posix() for p in sorted(files))+'\n'
archive=root/'installer/launcher.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=5) as z:
    for p in files:z.write(p,p.relative_to(launcher).as_posix())
    z.writestr('SHA256SUMS.txt',manifest)
out=root/f'dist/TecniHardcore-Setup-{version}.exe';out.parent.mkdir(exist_ok=True)
csc=Path('C:/Windows/Microsoft.NET/Framework64/v4.0.30319/csc.exe')
subprocess.run([str(csc),'/nologo','/target:winexe','/optimize+','/win32icon:'+str(root/'launcher/assets/icon.ico'),'/win32manifest:'+str(root/'installer/installer.manifest'),'/out:'+str(out),'/resource:'+str(archive)+',launcher.zip','/reference:System.Windows.Forms.dll','/reference:System.Drawing.dll','/reference:System.IO.Compression.dll','/reference:System.IO.Compression.FileSystem.dll',str(root/'installer/Installer.cs'),str(root/'installer/BuildInfo.cs')],check=True)
(out.parent/'SHA256SUMS.txt').write_text(hashlib.sha256(out.read_bytes()).hexdigest()+'  '+out.name+'\n')
print(f'Built portable installer: {out} ({out.stat().st_size//1048576} MiB)')
