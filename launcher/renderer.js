const { ipcRenderer } = require('electron');

// Estado local
let currentRam = localStorage.getItem('launcher_ram') || 6;
let currentUsername = '';
let customJavaPath = localStorage.getItem('launcher_javapath') || '';

// Elementos DOM
const btnMin = document.getElementById('btn-minimize');
const btnMax = document.getElementById('btn-maximize');
const btnClose = document.getElementById('btn-close');

const navItems = document.querySelectorAll('.nav-item');
const tabPanels = document.querySelectorAll('.tab-panel');

const usernameInput = document.getElementById('username-input');
const avatarPreview = document.getElementById('avatar-preview');
const ramSlider = document.getElementById('ram-slider');
const ramSliderDisplay = document.getElementById('ram-slider-display');
const dockRamVal = document.getElementById('dock-ram-val');
const dockServerVal = document.getElementById('dock-server-val');
const javaPathInput = document.getElementById('java-path-input');
const btnSaveSettings = document.getElementById('btn-save-settings');

const liveServerPill = document.getElementById('live-server-pill');
const serverStatusText = document.getElementById('server-status-text');
const serverPingText = document.getElementById('server-ping-text');
const serverOnlineState = document.getElementById('server-online-state');
const serverPingBig = document.getElementById('server-ping-big');
const playerLivesDisplay = document.getElementById('player-lives-display');
const playerLivesHearts = document.getElementById('player-lives-hearts');
const btnCopyIp = document.getElementById('btn-copy-ip');

const btnPlayGame = document.getElementById('btn-play-game');
const loadingScreen = document.getElementById('custom-loading-screen');
const loadingStatusMsg = document.getElementById('loading-status-msg');
const loadingProgressFill = document.getElementById('loading-progress-fill');
const loadingTaskName = document.getElementById('loading-task-name');
const loadingPercentage = document.getElementById('loading-percentage');
const btnCancelLaunch = document.getElementById('btn-cancel-launch');

// Auto-detectar Usuario Oficial de Minecraft (Doumoment)
let settingsReady=false;
const settingsLoaded=ipcRenderer.invoke('get-settings',{username:localStorage.getItem('launcher_username')||'',ram:localStorage.getItem('launcher_ram'),javaPath:localStorage.getItem('launcher_javapath')||''}).then((settings) => {
  currentUsername=settings.username;currentRam=settings.ram;customJavaPath=settings.javaPath;
  ramSlider.value=currentRam;ramSliderDisplay.textContent=currentRam+' GB';javaPathInput.value=customJavaPath;
  document.getElementById('game-folder').textContent=settings.gamePath;
  document.getElementById('graphics-mode').value=settings.graphicsMode||'vanilla';settingsReady=true;
  if (usernameInput) usernameInput.value = currentUsername;
  updateAvatar(currentUsername);
  updateLivesDisplay(currentUsername);
});

// Auto-detectar Java si no está establecido
ipcRenderer.invoke('detect-java').then((path) => {
  if (!customJavaPath) {
    customJavaPath = path;
  }
});

// Controles de ventana
if (btnMin) btnMin.addEventListener('click', () => ipcRenderer.send('window-min'));
if (btnMax) btnMax.addEventListener('click', () => ipcRenderer.send('window-max'));
if (btnClose) btnClose.addEventListener('click', () => ipcRenderer.send('window-close'));

// Cambio de Pestañas (JUGAR / SERVIDOR)
navItems.forEach((btn) => {
  btn.addEventListener('click', () => {
    const targetTab = btn.getAttribute('data-tab');
    navItems.forEach((b) => b.classList.remove('active'));
    tabPanels.forEach((p) => p.classList.remove('active'));

    btn.classList.add('active');
    const panel = document.getElementById(targetTab);
    if (panel) panel.classList.add('active');
  });
});

function updateAvatar(name) {
  if (avatarPreview) {
    avatarPreview.src = `https://minotar.net/avatar/${encodeURIComponent(name)}/64`;
  }
}

// RAM inicial
if (ramSlider) {
  ramSlider.value = currentRam;
  if (ramSliderDisplay) ramSliderDisplay.textContent = currentRam + ' GB';
  ramSlider.addEventListener('input', (e) => {
    currentRam = e.target.value;
    if (ramSliderDisplay) ramSliderDisplay.textContent = currentRam + ' GB';
    localStorage.setItem('launcher_ram', currentRam);
    ipcRenderer.invoke('save-settings',{ram:currentRam}).catch(error=>document.getElementById('graphics-status').textContent=error.message);
  });
}

if (btnSaveSettings && javaPathInput) {
  btnSaveSettings.addEventListener('click', () => {
    customJavaPath = javaPathInput.value.trim();
    localStorage.setItem('launcher_javapath', customJavaPath);
    ipcRenderer.invoke('save-settings',{javaPath:customJavaPath});
  });
}

if (btnCopyIp) {
  btnCopyIp.addEventListener('click', async () => {
    const connection=await ipcRenderer.invoke('get-connection');
    if(connection.error){btnCopyIp.textContent='Sin dirección';return;}
    await navigator.clipboard.writeText(connection.host+':'+connection.port);
    btnCopyIp.textContent = '¡Copiado!';
    setTimeout(() => { btnCopyIp.textContent = 'Copiar'; }, 2000);
  });
}

// Consultar Vidas del Jugador
async function updateLivesDisplay(user) {
  try {
    const livesInfo = await ipcRenderer.invoke('get-player-lives', user || currentUsername);
    document.getElementById('resurrection-count').textContent=Number.isInteger(livesInfo.resurrections)?String(livesInfo.resurrections):'SIN DATOS';
    if (playerLivesDisplay && playerLivesHearts) {
      playerLivesDisplay.textContent = livesInfo.display;
      playerLivesHearts.replaceChildren();
      for (let i = 0; i < livesInfo.max; i++) {
        const icon = document.createElement('img');
        icon.className = `life-crystal ${i < livesInfo.lives ? 'life-active' : 'life-spent'}`;
        icon.src = 'assets/revive_heart_crystal.png';
        icon.alt = i < livesInfo.lives ? 'Vida disponible' : 'Vida perdida';
        playerLivesHearts.appendChild(icon);
      }
      const caption = document.createElement('span');
      caption.className = 'life-caption';
      caption.textContent = livesInfo.description;
      playerLivesHearts.appendChild(caption);
      if (livesInfo.lives !== null && livesInfo.lives <= 1) {
        playerLivesDisplay.style.color = '#ff2a55';
      } else {
        playerLivesDisplay.style.color = '#ffb703';
      }
    }
  } catch (err) {
    console.error('Error fetching lives:', err);
  }
}

// Monitor de Ping en tiempo real al servidor
async function checkServerStatus() {
  try {
    const res = await ipcRenderer.invoke('ping-server');
    const panel=res.dashboard||{fresh:false,players:[],news:[],version:'SIN DATOS'};
    document.getElementById('online-count').textContent=Number.isInteger(panel.online)?`${panel.online} / ${panel.max??'—'}`:'SIN DATOS';
    document.getElementById('server-pack-versions').textContent=`Paquete del servidor: ${panel.version} · Launcher: ${res.launcherVersion||'SIN DATOS'}`;
    const roster=document.getElementById('online-roster');roster.replaceChildren();
    if(!panel.fresh||!panel.players.length)roster.textContent=panel.fresh?'No hay jugadores autenticados conectados.':'Sin datos vigentes.';
    for(const p of panel.players){const row=document.createElement('div');row.className='roster-player';const name=document.createElement('strong');name.textContent=p.name;const stats=document.createElement('span');stats.textContent=`${p.lives??'—'}/3 vidas · ${p.resurrections??'—'} resurrecciones`;row.append(name,stats);roster.append(row);}
    const news=document.getElementById('server-news');news.replaceChildren();if(!panel.fresh||!panel.news.length)news.textContent=panel.fresh?'Sin novedades publicadas.':'Sin datos vigentes.';
    for(const n of panel.news){const item=document.createElement('article');const title=document.createElement('h4');title.textContent=n.title;const body=document.createElement('p');body.textContent=n.body;item.append(title,body);news.append(item);}
    const dot = liveServerPill.querySelector('.status-indicator-dot');

    if (res.online) {
      dot.className = 'status-indicator-dot online';
      serverStatusText.textContent = 'Online';
      serverPingText.textContent = `${res.ping} ms`;
      serverOnlineState.textContent = 'ONLINE';
      serverOnlineState.style.color = '#06d6a0';
      serverPingBig.textContent = `Latencia: ${res.ping} ms`;
      dockServerVal.textContent = res.port+' ONLINE';
      dockServerVal.className = 'spec-val highlight';
    } else {
      dot.className = 'status-indicator-dot offline';
      serverStatusText.textContent = 'Offline';
      serverPingText.textContent = 'No responde';
      serverOnlineState.textContent = 'OFFLINE';
      serverOnlineState.style.color = '#ff2a55';
      serverPingBig.textContent = 'Servidor apagado';
      dockServerVal.textContent = 'SIN CONEXIÓN';
      dockServerVal.className = 'spec-val';
    }

    updateLivesDisplay(currentUsername);
  } catch (e) {
    console.error('Error pinging server:', e);
  }
}

// Sondeo periódico cada 4 segundos
checkServerStatus();
setInterval(checkServerStatus, 5000);

// =================================================================
// PANTALLA DE CARGA PERSONALIZADA ESTILO EUFONIA STUDIO
// =================================================================
// Real install/download/process events; no simulated progress.
let launchPending = false;
let withoutShaders=false;
btnPlayGame.addEventListener('click', async () => {
  if (launchPending) return;
  try{await settingsLoaded;}catch(error){document.getElementById('graphics-status').textContent=error.message;return;}
  if(!settingsReady||!/^[A-Za-z0-9_]{3,16}$/.test(usernameInput.value.trim())){usernameInput.focus();document.getElementById('graphics-status').textContent='Escribe y confirma tu nombre de Minecraft antes de jugar.';return;}
  launchPending = true;
  btnPlayGame.disabled = true;
  loadingScreen.classList.remove('hidden');
  loadingProgressFill.style.width = '0%';
  loadingPercentage.textContent = '…';
  loadingStatusMsg.textContent = 'Preparando TecniHardcore…';
  loadingTaskName.textContent = 'Minecraft 1.20.1 · Fabric';
  try {
    const result = await ipcRenderer.invoke('launch-game', {
      username: usernameInput.value.trim() || currentUsername,
      ram: Number(currentRam), javaPath: customJavaPath,withoutShaders
    });
    if (!result.success) throw new Error(result.error);
    withoutShaders=false;
    loadingStatusMsg.textContent = 'Minecraft está abierto';
    loadingTaskName.textContent = `${result.mods} mods · La carga continúa en la ventana del juego`;
    loadingProgressFill.style.width = '100%';
    loadingPercentage.textContent = '✓';
  } catch (error) {
    loadingStatusMsg.textContent = 'No se pudo iniciar Minecraft';
    loadingTaskName.textContent = error.message;
    launchPending = false;
    btnPlayGame.disabled = false;
  }
});
btnCancelLaunch.textContent = 'Volver al launcher';
btnCancelLaunch.addEventListener('click', () => loadingScreen.classList.add('hidden'));
ipcRenderer.on('launch-status', (_, data) => { loadingStatusMsg.textContent = data.message; });
ipcRenderer.on('launch-progress', (_, data) => {
  loadingProgressFill.style.width = `${data.percentage}%`;
  loadingPercentage.textContent = `${data.percentage}%`;
  loadingTaskName.textContent = `${data.type}: ${data.task} / ${data.total}`;
});
ipcRenderer.on('game-closed', (_, code) => {
  launchPending = false;
  btnPlayGame.disabled = false;
  if (code) {
    loadingScreen.classList.remove('hidden');
    loadingStatusMsg.textContent = 'Minecraft se cerró con un error';
    loadingTaskName.textContent = 'Consulta client/logs/latest.log y launcher.log.';
  } else loadingScreen.classList.add('hidden');
});

// =================================================================
// MOTOR DE PARTÍCULAS CANVAS (ASCUAS Y ESTRELLAS FLOTANTES)
// =================================================================
const canvas = document.getElementById('particles-canvas');
const ctx = canvas.getContext('2d');

let particles = [];
function resizeCanvas() {
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
}
window.addEventListener('resize', resizeCanvas);
resizeCanvas();

class Particle {
  constructor() {
    this.reset();
  }

  reset() {
    this.x = Math.random() * canvas.width;
    this.y = canvas.height + Math.random() * 20;
    this.size = Math.random() * 2.5 + 1;
    this.speedY = Math.random() * 1 + 0.3;
    this.speedX = (Math.random() - 0.5) * 0.6;
    this.opacity = Math.random() * 0.7 + 0.2;
    this.color = Math.random() > 0.6 ? '#ff2a55' : (Math.random() > 0.5 ? '#ffb703' : '#00f5d4');
  }

  update() {
    this.y -= this.speedY;
    this.x += this.speedX;
    this.opacity -= 0.002;
    if (this.y < -10 || this.opacity <= 0) {
      this.reset();
    }
  }

  draw() {
    ctx.save();
    ctx.globalAlpha = Math.max(0, this.opacity);
    ctx.fillStyle = this.color;
    ctx.shadowBlur = 8;
    ctx.shadowColor = this.color;
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }
}

for (let i = 0; i < 40; i++) {
  particles.push(new Particle());
}

function animateParticles() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  particles.forEach((p) => {
    p.update();
    p.draw();
  });
  requestAnimationFrame(animateParticles);
}
animateParticles();

usernameInput.addEventListener('change',async()=>{try{const settings=await ipcRenderer.invoke('save-settings',{username:usernameInput.value.trim()});currentUsername=settings.username;localStorage.setItem('launcher_username',currentUsername);updateAvatar(currentUsername);updateLivesDisplay(currentUsername);}catch(error){document.getElementById('graphics-status').textContent=error.message;}});
document.getElementById('graphics-mode').addEventListener('change',async event=>{const result=await ipcRenderer.invoke('set-graphics',event.target.value);document.getElementById('graphics-status').textContent=result.success?'Perfil seleccionado. Conserva los mods y tus otros ajustes.':result.error;});
document.getElementById('restore-graphics').addEventListener('click',async()=>{const result=await ipcRenderer.invoke('restore-graphics');document.getElementById('graphics-status').textContent=result.success?'Ajustes del perfil restaurados.':result.error;});
document.getElementById('choose-game-folder').addEventListener('click',async()=>{const result=await ipcRenderer.invoke('choose-game-folder');if(result.success)document.getElementById('game-folder').textContent=result.settings.gamePath;else if(!result.canceled)document.getElementById('folder-status').textContent=result.error;});
ipcRenderer.on('shader-failed',(_,data)=>{document.getElementById('graphics-status').textContent=data.message;document.getElementById('without-shaders').hidden=false;});
document.getElementById('without-shaders').addEventListener('click',()=>{withoutShaders=true;document.getElementById('without-shaders').hidden=true;btnPlayGame.click();});
ipcRenderer.invoke('get-connection').then(c=>{document.getElementById('connection-address').textContent=c.error||c.host+':'+c.port;});
const repairButton=document.getElementById('btn-repair');
repairButton.addEventListener('click',async()=>{repairButton.disabled=true;try{const result=await ipcRenderer.invoke('repair-client');document.getElementById('repair-status').textContent=result.success?'Paquete verificado y reparado.':result.error;}finally{repairButton.disabled=false;}});

const updateBanner=document.getElementById('update-banner');
const updateButton=document.getElementById('btn-update');
const cancelUpdate=document.getElementById('btn-cancel-update');
const updateStatus=document.getElementById('update-status');
const checkUpdate=document.getElementById('btn-check-update');
let updateBusy=false;
async function checkUpdates(){
  if(updateBusy)return;
  checkUpdate.disabled=true;
  try {
    const result=await ipcRenderer.invoke('check-update');
    if(!result.success){updateStatus.textContent=`No se pudo comprobar: ${result.error} Puedes seguir jugando.`;return;}
    updateStatus.textContent=`Launcher ${result.current} · ${result.available?'Hay una actualización disponible.':'Tienes la última versión.'}`;
    updateBanner.classList.toggle('hidden',!result.available);
    if(result.available){
      document.getElementById('update-title').textContent=`TecniHardcore ${result.manifest.version} disponible`;
      document.getElementById('update-notes').textContent=`${result.manifest.notes} · ${Math.ceil(result.manifest.installer.bytes/1048576)} MB. Cierra Minecraft para actualizar.`;
    }
  }finally{checkUpdate.disabled=false;}
}
checkUpdate.addEventListener('click',checkUpdates);
updateButton.addEventListener('click',async()=>{
  if(updateBusy)return;
  updateBusy=true;updateButton.disabled=true;cancelUpdate.classList.remove('hidden');
  document.getElementById('update-title').textContent='Descargando actualización…';
  try{
    const result=await ipcRenderer.invoke('install-update');
    if(!result.success){updateStatus.textContent=result.error;document.getElementById('update-notes').textContent=result.error;}
    else if(result.current){updateBanner.classList.add('hidden');await checkUpdates();}
  }finally{updateBusy=false;updateButton.disabled=false;cancelUpdate.classList.add('hidden');}
});
cancelUpdate.addEventListener('click',()=>ipcRenderer.invoke('cancel-update'));
ipcRenderer.on('update-progress',(_,progress)=>{
  document.getElementById('update-title').textContent=progress.message||`Descargando actualización · ${progress.percentage}%`;
  if(progress.total)document.getElementById('update-notes').textContent=`${Math.floor(progress.received/1048576)} / ${Math.ceil(progress.total/1048576)} MB. Se verificará SHA-256 antes de instalar.`;
});
checkUpdates();
setInterval(checkUpdates,60*60*1000);
