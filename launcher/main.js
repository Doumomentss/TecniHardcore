const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron');
const path = require('path');
const fs = require('fs');
const {getConnection} = require('./connection');
const serverStatus = require('./server-status');
const { execSync, spawn } = require('child_process');
const { Client, Authenticator } = require('minecraft-launcher-core');

let mainWindow;
let preferences;
try{preferences=new (require('./settings').Settings)(path.join(app.getPath('appData'),'TecniHardcore'));}catch(error){dialog.showErrorBox('Ajustes de TecniHardcore',error.message+'\n'+path.join(app.getPath('appData'),'TecniHardcore','settings.json'));app.exit(1);throw error;}
const graphics=require('./graphics');
function desktopShortcut(){if(!app.isPackaged||process.platform!=='win32')return;try{const link=path.join(app.getPath('desktop'),'TecniHardcore.lnk');if(!shell.writeShortcutLink(link,fs.existsSync(link)?'update':'create',{target:process.execPath,cwd:path.dirname(process.execPath),icon:process.execPath,iconIndex:0,description:'TecniHardcore',appUserModelId:'com.tecnihardcore.launcher'}))console.warn('No se pudo crear el acceso directo.');}catch(error){console.warn('Acceso directo: '+error.message);}}


if (process.platform === 'win32') {
  app.setAppUserModelId('com.tecnihardcore.launcher');
}

const iconFile = process.platform === 'win32'
  ? path.join(__dirname, 'assets', 'icon.ico')
  : path.join(__dirname, 'assets', 'tecnihardcore_logo.png');

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1100,
    height: 700,
    minWidth: 950,
    minHeight: 620,
    frame: false,
    transparent: false,
    backgroundColor: '#0a0b10',
    icon: iconFile,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'index.html'));

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(()=>{createWindow();desktopShortcut();});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// Controles de Ventana
ipcMain.on('window-min', () => {
  if (mainWindow) mainWindow.minimize();
});

ipcMain.on('window-max', () => {
  if (mainWindow) {
    if (mainWindow.isMaximized()) {
      mainWindow.unmaximize();
    } else {
      mainWindow.maximize();
    }
  }
});

ipcMain.on('window-close', () => {
  if (mainWindow) mainWindow.close();
});

// Auto-detectar usuario de Minecraft (usercache.json o launcher_profiles.json)
ipcMain.handle('detect-user', async () => {
  const mcRoot = path.join(process.env.APPDATA || '', '.minecraft');
  const usercachePath = path.join(mcRoot, 'usercache.json');
  const profilesPath = path.join(mcRoot, 'launcher_profiles.json');

  try {
    if (fs.existsSync(usercachePath)) {
      const data = JSON.parse(fs.readFileSync(usercachePath, 'utf-8'));
      if (Array.isArray(data) && data.length > 0 && data[0].name) {
        return data[0].name;
      }
    }
  } catch (e) {}

  try {
    if (fs.existsSync(profilesPath)) {
      const profiles = JSON.parse(fs.readFileSync(profilesPath, 'utf-8'));
      const name=profiles.authenticationDatabase?.[profiles.selectedUser?.account]?.profiles?.[profiles.selectedUser?.profile]?.displayName;
      if(require('./settings').validName(name))return name;
    }
  } catch (e) {}

  return '';
});

// Auto-detectar Java Runtime
ipcMain.handle('detect-java', async () => { try { return require('./client-install').findJava(); } catch (_) { return ''; } });

// The Minecraft status port is the only public source of life data.
ipcMain.handle('get-connection', () => { try { return getConnection(process.resourcesPath); } catch(error) { return {error:error.message}; } });
ipcMain.handle('get-player-lives', async (event, username) => {
  try { return serverStatus.livesFrom(await serverStatus.snapshot(getConnection(process.resourcesPath)),String(username||'')); }
  catch(error) { return {lives:null,max:5,display:'SIN DATOS',description:error.message}; }
});
ipcMain.handle('ping-server', async () => {
  try { const result=await serverStatus.snapshot(getConnection(process.resourcesPath)); return {...result,dashboard:serverStatus.dashboard(result),launcherVersion:app.getVersion()}; }
  catch(error) { return {online:false,error:error.message}; }
});

// A single, isolated Fabric client; never mix with the user's other Minecraft versions.
const installation = require('./client-install');
const updater = require('./updater');
let gameProcess = null;
let preparing = false;
let updating = false;
let updateDownload = null;
function clientRoot(payload) {
  if(preferences.data.gamePath)return preferences.data.gamePath;
  const ownerRoot=path.resolve(process.resourcesPath,'../..');
  if(fs.existsSync(path.join(ownerRoot,'connection.local.json')))return path.join(ownerRoot,'client');
  return path.basename(payload)==='installer_payload'?path.join(path.dirname(payload),'client'):path.join(path.dirname(process.execPath),'game');
}
const send = (channel, value) => {
  if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send(channel, value);
};
ipcMain.handle('get-settings',(_,legacy={})=>{const payload=installation.findPayload(process.resourcesPath);return preferences.migrate(legacy,clientRoot(payload));});
ipcMain.handle('save-settings',(_,patch)=>preferences.save(patch));
ipcMain.handle('choose-game-folder',async()=>{
 if(preparing||gameProcess||updating)return {success:false,error:'Cierra Minecraft y espera a que termine la actualización.'};
 const result=await dialog.showOpenDialog(mainWindow,{title:'Carpeta de Minecraft TecniHardcore',properties:['openDirectory','createDirectory']});if(result.canceled)return {success:false,canceled:true};
 const source=clientRoot(installation.findPayload(process.resourcesPath)),target=result.filePaths[0];
 try{require('./game-folder').safe(source,target);const existing=fs.existsSync(path.join(target,'options.txt'));
 const choice=await dialog.showMessageBox(mainWindow,{type:'question',message:existing?'Encontramos Minecraft en esta carpeta.':'¿Cómo quieres usar esta carpeta?',detail:existing?'Se verificará el paquete y se conservarán sus archivos.':'Trasladar crea y verifica una copia; conserva también la carpeta original.',buttons:existing?['Usar y verificar','Cancelar']:['Trasladar y verificar','Cancelar'],cancelId:1});if(choice.response===1)return {success:false,canceled:true};
 preparing=true;send('launch-status',{message:'Verificando la carpeta del juego…'});if(!existing&&fs.existsSync(source))await require('./game-folder').transfer(source,target);
 await installation.prepareClient(target,installation.findPayload(process.resourcesPath),m=>send('launch-status',{message:m}),getConnection(process.resourcesPath));preferences.save({gamePath:target});return {success:true,settings:preferences.data};
 }catch(error){return {success:false,error:error.message};}finally{preparing=false;}
});
async function selectGraphics(mode){if(preparing||gameProcess||updating)return {success:false,error:'Cierra Minecraft antes de cambiar el perfil gráfico.'};preparing=true;try{preferences.save({graphicsMode:mode,graphicsPending:true});const payload=installation.findPayload(process.resourcesPath),root=clientRoot(payload);if(fs.existsSync(path.join(root,'options.txt'))){await graphics.apply(root,mode,payload,m=>send('launch-status',{message:m}));preferences.save({graphicsPending:false});}return {success:true,settings:preferences.data};}catch(error){return {success:false,error:error.message};}finally{preparing=false;}}
ipcMain.handle('set-graphics',(_,mode)=>selectGraphics(mode));
ipcMain.handle('restore-graphics',()=>selectGraphics(preferences.data.graphicsMode||'optimized'));
ipcMain.handle('repair-client',async()=>{
  if(preparing||gameProcess||updating)return {success:false,error:'Cierra Minecraft y espera a que termine la actualización antes de reparar.'};
  preparing=true;
  try{
    const payload=installation.findPayload(process.resourcesPath);
    const root=clientRoot(payload);
    await installation.prepareClient(root,payload,message=>send('launch-status',{message}),getConnection(process.resourcesPath));
    return {success:true};
  }catch(error){return {success:false,error:error.message};}finally{preparing=false;}
});
ipcMain.handle('launch-game', async (event, options = {}) => {
  if (preparing || gameProcess || updating) return { success: false, error: 'Espera a que termine la actualización o cierra Minecraft si ya está abierto.' };
  preparing = true;
  try {
    const payload = installation.findPayload(process.resourcesPath);
    const root = clientRoot(payload);
    const username = String(options.username || preferences.data.username || '').trim();
    if (!/^[A-Za-z0-9_]{3,16}$/.test(username)) throw new Error('Usa un nombre de 3 a 16 letras, números o guiones bajos.');
    preferences.save({username,ram:options.ram??preferences.data.ram,javaPath:options.javaPath??preferences.data.javaPath});
    const status = message => send('launch-status', { message });
    const endpoint = getConnection(process.resourcesPath);
    const java = await installation.ensureJava(root,options.javaPath,status);
    const installed = await installation.prepareClient(root, payload, status,endpoint);
    if(options.withoutShaders)await graphics.disableOnce(root);
    else if(preferences.data.graphicsPending){await graphics.apply(root,preferences.data.graphicsMode,payload,status);preferences.save({graphicsPending:false});}
    else if(graphics.PRESETS[preferences.data.graphicsMode]?.shader)await graphics.ensureShader(root,path.join(payload,'shaders-download.json'),status);
    const ram = Math.min(16, Math.max(2, Number(options.ram) || 6));
    const launcher = new Client();
    fs.mkdirSync(path.join(root, 'logs'), { recursive: true });
    const logFile = path.join(root, 'logs', 'launcher.log');
    fs.writeFileSync(logFile, `${new Date().toISOString()} Minecraft ${installation.VERSION}; Fabric ${installation.PROFILE}; ${installed.mods} mods; Java ${java}\n`);
    let lastError = '',shaderFailure=false;
    launcher.on('debug', message => {
      // Do not persist launch arguments, which can contain authentication tokens.
      if (!message.includes('Launching with arguments')) fs.appendFileSync(logFile, message + '\n');
      if (/Failed|Couldn.t start/i.test(message)) lastError = message;
    });
    launcher.on('progress', e => send('launch-progress', { ...e, percentage: Math.round(e.task / Math.max(1,e.total) * 100) }));
    launcher.on('data', data => {
      if(graphics.shaderError(data.toString()))shaderFailure=true;
      fs.appendFileSync(logFile, data.toString());
      send('game-log', data.toString());
    });
    launcher.on('close', code => { gameProcess = null; send('game-closed', code);if(shaderFailure)send('shader-failed',{message:'No se pudieron compilar los shaders. Puedes reiniciar sin shaders; conservaremos tu perfil.'}); });
    status(`Iniciando Minecraft 1.20.1 con Fabric y ${installed.mods} mods…`);
    const versionJson = path.join(root,'versions','1.20.1','1.20.1.json');
    const child = await launcher.launch({
      authorization: Authenticator.getAuth(username), root, javaPath: java,
      version: { number: installation.VERSION, type: 'release', custom: installation.PROFILE },
      memory: { max: ram+'G', min: '2G' },
      window: { width: 1280, height: 720 },
      customArgs: ['-XX:+UseG1GC', '-XX:+ParallelRefProcEnabled', '-XX:MaxGCPauseMillis=200'],
      overrides: { detached: false, ...(fs.existsSync(versionJson) ? { versionJson } : {}) }
    });
    if (!child || !child.pid) throw new Error(lastError || 'Minecraft no pudo iniciarse. Consulta client/logs/launcher.log.');
    gameProcess = child;
    child.on('error', error => { gameProcess = null; send('launch-status', {message:error.message}); send('game-closed',1); });
    return { success: true, root, mods: installed.mods };
  } catch (error) {
    return { success: false, error: error.message };
  } finally { preparing = false; }
});

ipcMain.handle('check-update', async () => {
  try {return {success:true,...await updater.check(app.getVersion())};}
  catch(error){return {success:false,current:app.getVersion(),error:error.message};}
});
ipcMain.handle('cancel-update', () => {if(updateDownload)updateDownload.abort();});
ipcMain.handle('install-update', async () => {
  if(preparing||gameProcess||updating)return {success:false,error:'Cierra Minecraft antes de actualizar el launcher.'};
  if(!app.isPackaged)return {success:false,error:'Las actualizaciones se instalan desde el launcher instalado, no desde el código fuente.'};
  updating=true;
  updateDownload=new AbortController();
  try {
    // Consult again: the renderer never chooses a URL or supplies executable paths.
    const result=await updater.check(app.getVersion());
    if(!result.available)return {success:true,current:true};
    const directory=path.join(app.getPath('userData'),'updates',result.manifest.version);
    const file=await updater.download(result.manifest,directory,progress=>send('update-progress',progress),updateDownload.signal);
    send('update-progress',{percentage:100,message:'Descarga verificada. Se abrirá el instalador; espera a que vuelva el launcher.'});
    const helper=spawn(file,['--update',path.dirname(process.execPath),String(process.pid)],{detached:true,stdio:'ignore',windowsHide:false});
    await new Promise((resolve,reject)=>{helper.once('spawn',resolve);helper.once('error',reject);});
    helper.unref();
    app.quit();
    return {success:true};
  } catch(error) {
    return {success:false,error:error.name==='AbortError'?'Descarga cancelada.':error.message};
  } finally {updating=false;updateDownload=null;}
});
