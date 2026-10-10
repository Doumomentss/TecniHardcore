const fs = require('fs');
const fsp = fs.promises;
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const VERSION = '1.20.1';
const PROFILE = 'fabric-loader-0.19.5-1.20.1';
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');

function findPayload(resourcesPath) {
  const candidates = [path.join(resourcesPath, 'app.asar', 'pack'), path.resolve(__dirname, '../installer_payload'),
    path.resolve(resourcesPath, '../../installer_payload'),
    path.join(resourcesPath, 'pack'),
    path.join(resourcesPath, 'app.asar', 'pack'),
    path.join(__dirname, 'pack')];
  const found = candidates.find(p => fs.existsSync(path.join(p, 'mods')));
  if (!found) throw new Error('Falta el paquete de mods y menú. Reinstala el launcher completo.');
  return found;
}

function findJava(preferred) {
  const candidates = [preferred, process.env.JAVA_HOME && path.join(process.env.JAVA_HOME, 'bin/java.exe'),
    ...['Eclipse Adoptium','Java','BellSoft'].flatMap(v => {
      const dir=path.join(process.env.ProgramFiles||'C:/Program Files',v);
      return fs.existsSync(dir)?fs.readdirSync(dir).map(n=>path.join(dir,n,'bin','java.exe')):[];
    }), 'java'].filter(Boolean);
  for (const candidate of [...new Set(candidates)]) {
    try {
      const settings = require('child_process').spawnSync(candidate, ['-XshowSettings:properties','-version'], {encoding:'utf8',windowsHide:true});
      const javaVersion = String(settings.stderr).match(/java\.version\s*=\s*(\d+)/);
      if (settings.status === 0 && javaVersion && Number(javaVersion[1]) === 17) return candidate;
    } catch (_) { /* Try next installed runtime. */ }
  }
  throw new Error('No se encontró Java 17. El launcher instalará el runtime oficial automáticamente.');
}

async function ensureJava(root,preferred,status) {
  const runtime=path.join(root,'runtime');
  const candidates=fs.existsSync(runtime)?fs.readdirSync(runtime).map(n=>path.join(runtime,n,'bin/java.exe')):[];
  for(const candidate of [preferred,...candidates]) {if(!candidate)continue;try{return findJava(candidate);}catch(_) {}}
  try{return findJava();}catch(_){}
  status('Descargando Java 17 oficial y verificando su integridad…');
  const info=await fetch('https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse');
  if(!info.ok)throw Error('No se pudo consultar Java 17.');
  const pkg=(await info.json())[0]?.binary?.package;
  if(!pkg?.link||!pkg?.checksum)throw Error('Metadatos de Java inválidos.');
  const result=await fetch(pkg.link);if(!result.ok)throw Error('No se pudo descargar Java 17.');
  const bytes=Buffer.from(await result.arrayBuffer());
  if(crypto.createHash('sha256').update(bytes).digest('hex')!==pkg.checksum)throw Error('La descarga de Java no superó la verificación.');
  await fsp.mkdir(runtime,{recursive:true});const archive=path.join(runtime,'java17.zip');await fsp.writeFile(archive,bytes);
  await require('util').promisify(require('child_process').execFile)('tar.exe',['-xf',archive,'-C',runtime],{windowsHide:true});
  await fsp.unlink(archive);
  const java=fs.readdirSync(runtime).map(n=>path.join(runtime,n,'bin/java.exe')).find(fs.existsSync);
  if(!java)throw Error('Java 17 no se pudo instalar.');return findJava(java);
}

async function prepareClient(root, payload, status, endpoint) {
  if(endpoint&&(!/^[a-zA-Z0-9.:-]+$/.test(endpoint.host||'')||!Number.isInteger(endpoint.port)||endpoint.port<1||endpoint.port>65535))throw new Error('Dirección de conexión inválida. Revisa el perfil del servidor antes de reparar.');
  await fsp.mkdir(root, {recursive:true});
  const shared = path.join(process.env.APPDATA || '', '.minecraft');
  const profile = path.join(root,'versions',PROFILE,`${PROFILE}.json`);
  if (!fs.existsSync(profile)) {
    status('Preparando la instalación independiente de Minecraft 1.20.1…');
    for (const dir of ['assets','libraries',`versions/${VERSION}`,`versions/${PROFILE}`]) {
      const source = path.join(shared,dir);
      if (fs.existsSync(source)) await fsp.cp(source,path.join(root,dir),{recursive:true,force:false,errorOnExist:false});
    }
    if (!fs.existsSync(profile)) {
      const response = await fetch(`https://meta.fabricmc.net/v2/versions/loader/${VERSION}/0.19.5/profile/json`);
      if (!response.ok) throw new Error(`No se pudo descargar Fabric (${response.status}).`);
      await fsp.mkdir(path.dirname(profile),{recursive:true});
      await fsp.writeFile(profile, JSON.stringify(await response.json()));
    }
  }
  const fabric = JSON.parse(await fsp.readFile(profile,'utf8'));
  if (fabric.inheritsFrom !== VERSION || fabric.mainClass !== 'net.fabricmc.loader.impl.launch.knot.KnotClient') {
    throw new Error('El perfil de Fabric no corresponde a Minecraft 1.20.1.');
  }
  const statePath=path.join(root,'tecnihardcore-managed.json');
  const old=fs.existsSync(statePath) ? JSON.parse(await fsp.readFile(statePath,'utf8')) : {};
  const next={};
  const downloadsFile=path.join(payload,'mods-downloads.json');
  const downloads=fs.existsSync(downloadsFile)?JSON.parse(await fsp.readFile(downloadsFile,'utf8')).files:[];
  if(!Array.isArray(downloads))throw Error('Falta el catálogo de descargas oficiales de mods.');
  for(const mod of downloads){
    if(!/^[A-Za-z0-9_.+() -]+\.jar$/.test(mod.name)||!mod.url?.startsWith('https://cdn.modrinth.com/data/')||!/^([a-f0-9]{128})$/.test(mod.sha512)||!/^([a-f0-9]{64})$/.test(mod.sha256)||!Number.isSafeInteger(mod.bytes)||mod.bytes<1)throw Error('Catálogo de mods inválido.');
    next[path.join('mods',mod.name)]=mod.sha256;
  }
  async function sync(dir) {
    for (const entry of await fsp.readdir(path.join(payload,dir),{withFileTypes:true})) {
      const relative=path.join(dir,entry.name);
      if (entry.isDirectory()) { await sync(relative); continue; }
      const src=path.join(payload,relative), dest=path.join(root,relative);
      let bytes=await fsp.readFile(src);
      if(endpoint && /^config\/fancymenu\/customization\/[^/]+\.txt$/.test(relative.replace(/\\/g,'/'))) {
        bytes=Buffer.from(bytes.toString('utf8').replace(/(\[action_type:joinserver\]\s*=\s*)[^\r\n]+/g,'$1'+endpoint.host+':'+endpoint.port));
      }
      next[relative]=crypto.createHash('sha256').update(bytes).digest('hex');
      if (!fs.existsSync(dest) || hash(dest) !== next[relative]) {
        if (fs.existsSync(dest)) {
          const backup=path.join(root,'backups',String(Date.now()),relative);
          await fsp.mkdir(path.dirname(backup),{recursive:true}); await fsp.copyFile(dest,backup);
        }
        await fsp.mkdir(path.dirname(dest),{recursive:true});
        const temp=dest+'.tecni-tmp';await fsp.writeFile(temp,bytes);await fsp.rename(temp,dest);
      }
    }
  }
  status('Sincronizando el paquete completo de mods y el menú…');
  const shippedMods=new Set([...(await fsp.readdir(path.join(payload,'mods'))),...downloads.map(m=>m.name)]);
  const modsPath=path.join(root,'mods');
  if(fs.existsSync(modsPath))for(const name of await fsp.readdir(modsPath)){
    if(/^tecnihardcore-\d+\.\d+\.\d+\.jar$/i.test(name)&&!shippedMods.has(name)){
      const backup=path.join(root,'backups',String(Date.now()),'mods',name);
      await fsp.mkdir(path.dirname(backup),{recursive:true});await fsp.rename(path.join(modsPath,name),backup);
    }
  }
  let completed=0;
  const queue=[...downloads];
  async function downloadWorker(){
    while(queue.length){
      const mod=queue.shift(),dest=path.join(modsPath,mod.name);
      if(!fs.existsSync(dest)||hash(dest)!==mod.sha256){
        status(`Descargando mod oficial (${completed+1}/${downloads.length}): ${mod.name}`);
        const response=await fetch(mod.url,{signal:AbortSignal.timeout(180000)});
        if(!response.ok)throw Error(`No se pudo descargar ${mod.name} (${response.status}). Pulsa Reparar para volver a intentar.`);
        const bytes=Buffer.from(await response.arrayBuffer());
        if(bytes.length!==mod.bytes||crypto.createHash('sha512').update(bytes).digest('hex')!==mod.sha512||crypto.createHash('sha256').update(bytes).digest('hex')!==mod.sha256)throw Error(`Falló la verificación de ${mod.name}. Vuelve a intentar la reparación.`);
        if(fs.existsSync(dest)){
          const backup=path.join(root,'backups',String(Date.now()),'mods',mod.name);
          await fsp.mkdir(path.dirname(backup),{recursive:true});await fsp.copyFile(dest,backup);
        }
        await fsp.mkdir(modsPath,{recursive:true});
        const temporary=dest+'.tecni-tmp';await fsp.writeFile(temporary,bytes);await fsp.rename(temporary,dest);
      }
      completed++;
    }
  }
  // Wait for every worker before reporting failure; a retry must never race a previous download.
  const workers=await Promise.allSettled(Array.from({length:4},downloadWorker));
  const failure=workers.find(worker=>worker.status==='rejected');if(failure)throw failure.reason;
  await sync('mods'); await sync('config'); await sync('resourcepacks');
  // Keep the official client set reproducible. Personal additions are backed up, never deleted.
  const shaderNames=new Set();
  for(const catalog of ['shaders-download.json','optional-shaders.json']){
    const file=path.join(payload,catalog);if(!fs.existsSync(file))continue;
    const data=JSON.parse(await fsp.readFile(file,'utf8'));
    for(const item of Array.isArray(data.files)?data.files:[data])if(item.name)shaderNames.add(item.name);
  }
  for(const [folder,allowed] of [['mods',shippedMods],['resourcepacks',new Set(await fsp.readdir(path.join(payload,'resourcepacks')))],['shaderpacks',shaderNames]]){
    const directory=path.join(root,folder);if(!fs.existsSync(directory))continue;
    for(const name of await fsp.readdir(directory)){
      if(allowed.has(name)||(folder==='shaderpacks'&&name.endsWith('.txt')&&shaderNames.has(name.slice(0,-4))))continue;
      const source=path.join(directory,name),backup=path.join(root,'backups',String(Date.now()),folder,name);
      await fsp.mkdir(path.dirname(backup),{recursive:true});await fsp.rename(source,backup);
      status(`Contenido ajeno al paquete guardado en respaldos: ${folder}/${name}`);
    }
  }
  for(const relative of Object.keys(old)) {
    if (!next[relative] && relative.startsWith('mods'+path.sep) && fs.existsSync(path.join(root,relative))) {
      const dest=path.join(root,'backups',String(Date.now()),relative);
      await fsp.mkdir(path.dirname(dest),{recursive:true}); await fsp.rename(path.join(root,relative),dest);
    }
  }
  if (!fs.existsSync(path.join(root,'servers.dat'))) await fsp.copyFile(path.join(payload,'servers.dat'),path.join(root,'servers.dat'));
  if (!fs.existsSync(path.join(root,'options.txt'))) {
    await fsp.writeFile(path.join(root,'options.txt'),'version:3465\nlang:es_es\nguiScale:2\nfullscreen:false\nrenderDistance:12\nsimulationDistance:8\n');
  }
  const optionsPath=path.join(root,'options.txt');
  let options=await fsp.readFile(optionsPath,'utf8');
  const packLine=options.match(/^resourcePacks:(.*)$/m);let packs=[];
  try{packs=packLine?JSON.parse(packLine[1]):[];}catch(_){}
  if(!packs.includes('file/TecniHardcore_Pack'))packs.push('file/TecniHardcore_Pack');
  const newLine='resourcePacks:'+JSON.stringify(packs);
  options=packLine?options.replace(/^resourcePacks:.*$/m,newLine):options+'\n'+newLine+'\n';
  await fsp.writeFile(optionsPath,options);
  await fsp.writeFile(statePath,JSON.stringify(next,null,2));
  return {version:VERSION,profile:PROFILE,mods:Object.keys(next).filter(p=>p.startsWith('mods'+path.sep)).length};
}
module.exports={VERSION,PROFILE,findPayload,findJava,ensureJava,prepareClient};
