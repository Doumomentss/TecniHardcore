const {app,BrowserWindow}=require('electron');
const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve(__dirname,'..'),dir=path.join(__dirname,'test-runtime','update-ui');fs.mkdirSync(dir,{recursive:true});
app.setPath('userData',dir);app.getVersion=()=> '2.1.0';
let offline=false;const originalFetch=global.fetch;
global.fetch=(url,options)=>{
 if(url===require('../launcher/updater').FEED){if(offline)return Promise.reject(Error('Sin conexión de prueba'));return Promise.resolve(new Response(fs.readFileSync(path.join(root,'dist','update.json'),'utf8')));}
 return originalFetch(url,options);
};
require('../launcher/main');
app.whenReady().then(async()=>{
 try{
  const window=BrowserWindow.getAllWindows()[0];
  await new Promise(resolve=>window.webContents.once('did-finish-load',resolve));
  await new Promise(resolve=>setTimeout(resolve,1800));
  let state=await window.webContents.executeJavaScript(`({visible:!document.getElementById('update-banner').classList.contains('hidden'),title:document.getElementById('update-title').textContent,cancelHidden:getComputedStyle(document.getElementById('btn-cancel-update')).display==='none',status:document.getElementById('update-status').textContent})`);
  assert(state.visible);assert(state.title.includes(require('../launcher/package.json').version));assert(state.cancelHidden);assert(state.status.includes('2.1.0'));
  fs.writeFileSync(path.join(dir,'update-available.png'),(await window.webContents.capturePage()).toPNG());
  offline=true;
  await window.webContents.executeJavaScript("document.querySelector('[data-tab=tab-settings]').click();document.getElementById('btn-check-update').click()");
  await new Promise(resolve=>setTimeout(resolve,300));
  const status=await window.webContents.executeJavaScript("document.getElementById('update-status').textContent");
  assert(status.includes('Puedes seguir jugando'));
  fs.writeFileSync(path.join(dir,'offline-settings.png'),(await window.webContents.capturePage()).toPNG());
  fs.writeFileSync(path.join(dir,'result.json'),JSON.stringify({banner:true,version:true,offlinePlayable:true,cancelHidden:true},null,2));
  console.log('PASS: native Electron update banner, version, settings and useful offline message');
 }catch(error){console.error(error);process.exitCode=1;}finally{app.quit();}
});
