// Bootstrap a source checkout using the pinned, already published official release.
const fs=require('fs'),path=require('path'),crypto=require('crypto');
const root=path.resolve(__dirname,'..');
(async()=>{
 const release=JSON.parse(fs.readFileSync(path.join(root,'release/update.json')));
 const manifest=JSON.parse(fs.readFileSync(path.join(root,`release/manifest-${release.version}.json`)));
 const mods=path.join(root,'installer_payload/mods');fs.mkdirSync(mods,{recursive:true});
 for(const [name,info] of Object.entries(manifest.files).filter(([name])=>name.endsWith('.jar'))){
  const dest=path.join(mods,name);if(fs.existsSync(dest))continue;
  const url=`https://github.com/Doumomentss/TecniHardcore/releases/download/v${release.version}/${name}`;
  const response=await fetch(url);if(!response.ok)throw Error(`Download failed: ${name} (${response.status})`);
  const bytes=Buffer.from(await response.arrayBuffer());
  if(bytes.length!==info.bytes||crypto.createHash('sha256').update(bytes).digest('hex')!==info.sha256)throw Error('Release hash mismatch: '+name);
  fs.writeFileSync(dest,bytes);console.log('Verified own mod:',name);
 }
 const Zip=require('../launcher/node_modules/adm-zip');
 const deathJar=fs.readdirSync(mods).find(name=>name.startsWith('tecni-death-overlay-'));
 const animation=new Zip(path.join(mods,deathJar)).readFile('assets/tecni_death/death.fma');
 if(!animation)throw Error('Death animation missing');
 fs.writeFileSync(path.join(root,'installer_payload/config/fancymenu/assets/video_sin_fondo_transparente.fma'),animation);
 const portable=path.join(root,'TecniHardcore Launcher');
 if(!fs.existsSync(path.join(portable,'TecniHardcore Launcher.exe'))){
  await fs.promises.cp(path.join(root,'launcher/node_modules/electron/dist'),portable,{recursive:true});
  fs.renameSync(path.join(portable,'electron.exe'),path.join(portable,'TecniHardcore Launcher.exe'));
  const rcedit=require('../launcher/node_modules/rcedit');
  await rcedit(path.join(portable,'TecniHardcore Launcher.exe'),{icon:path.join(root,'launcher/assets/icon.ico'),'version-string':{'ProductName':'TecniHardcore','FileDescription':'TecniHardcore Launcher'}});
 }
 console.log('Payload and Windows runtime ready. Build your changed mod, then package.');
})().catch(error=>{console.error(error);process.exitCode=1;});
