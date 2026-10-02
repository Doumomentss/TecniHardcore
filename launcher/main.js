const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const {getConnection} = require('./connection');
const serverStatus = require('./server-status');
const { execSync, spawn } = require('child_process');
const { Client, Authenticator } = require('minecraft-launcher-core');

let mainWindow;

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

app.whenReady().then(createWindow);

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
      if (profiles.selectedUser && profiles.selectedUser.account) {
        return profiles.selectedUser.account;
      }
    }
  } catch (e) {}

  return 'Jugador';
});

// Auto-detectar Java Runtime
ipcMain.handle('detect-java', async () => { try { return require('./client-install').findJava(); } catch (_) { return ''; } });

// The Minecraft status port is the only public source of life data.
ipcMain.handle('get-connection', () => { try { return getConnection(process.resourcesPath); } catch(error) { return {error:error.message}; } });
ipcMain.handle('get-player-lives', async (event, username) => {
  try { return serverStatus.livesFrom(await serverStatus.snapshot(getConnection(process.resourcesPath)),String(username||'')); }
  catch(error) { return {lives:null,max:3,display:'SIN DATOS',description:error.message}; }
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
  const ownerRoot=path.resolve(process.resourcesPath,'../..');
  if(fs.existsSync(path.join(ownerRoot,'connection.local.json')))return path.join(ownerRoot,'client');
  return path.basename(payload)==='installer_payload'?path.join(path.dirname(payload),'client'):path.join(path.dirname(process.execPath),'game');
}
const send = (channel, value) => {
  if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send(channel, value);
};
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
    const username = String(options.username || 'Jugador').trim();
    if (!/^[A-Za-z0-9_]{3,16}$/.test(username)) throw new Error('Usa un nombre de 3 a 16 letras, números o guiones bajos.');
    const status = message => send('launch-status', { message });
    const endpoint = getConnection(process.resourcesPath);
    const java = await installation.ensureJava(root,options.javaPath,status);
    const installed = await installation.prepareClient(root, payload, status,endpoint);
    const ram = Math.min(16, Math.max(2, Number(options.ram) || 6));
    const launcher = new Client();
    fs.mkdirSync(path.join(root, 'logs'), { recursive: true });
    const logFile = path.join(root, 'logs', 'launcher.log');
    fs.writeFileSync(logFile, `${new Date().toISOString()} Minecraft ${installation.VERSION}; Fabric ${installation.PROFILE}; ${installed.mods} mods; Java ${java}\n`);
    let lastError = '';
    launcher.on('debug', message => {
      // Do not persist launch arguments, which can contain authentication tokens.
      if (!message.includes('Launching with arguments')) fs.appendFileSync(logFile, message + '\n');
      if (/Failed|Couldn.t start/i.test(message)) lastError = message;
    });
    launcher.on('progress', e => send('launch-progress', { ...e, percentage: Math.round(e.task / Math.max(1,e.total) * 100) }));
    launcher.on('data', data => {
      fs.appendFileSync(logFile, data.toString());
      send('game-log', data.toString());
    });
    launcher.on('close', code => { gameProcess = null; send('game-closed', code); });
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
    send('update-progress',{percentage:100,message:'Verificado. Cerrando el launcher para instalar…'});
    const helper=spawn(file,['--update',path.dirname(process.execPath),String(process.pid)],{detached:true,stdio:'ignore',windowsHide:true});
    await new Promise((resolve,reject)=>{helper.once('spawn',resolve);helper.once('error',reject);});
    helper.unref();
    app.quit();
    return {success:true};
  } catch(error) {
    return {success:false,error:error.name==='AbortError'?'Descarga cancelada.':error.message};
  } finally {updating=false;updateDownload=null;}
});
