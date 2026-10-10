const fs=require('fs'),path=require('path');
const {Client,Authenticator}=require('../launcher/node_modules/minecraft-launcher-core');
const install=require('../launcher/client-install');
const root=path.resolve(__dirname,'test-runtime/season29-game');
const server=path.resolve(__dirname,'test-runtime/season29-seed4');
const name=process.env.TECNI_QA_NAME||'E25IntroA';
if(!/^E25[A-Za-z0-9_]{1,13}$/.test(name))throw Error('Only isolated test accounts allowed');
if(!fs.existsSync(path.join(server,'.tecni-test-world')))throw Error('Isolated server marker required');
if(!fs.existsSync(path.join(root,'mods/tecnihardcore-2.9.0.jar')))throw Error('2.9.0 QA mod absent');
(async()=>{
 const java=await install.ensureJava(root,null,console.log),client=new Client(),log=path.join(root,'qa-season29.log');
 client.on('debug',m=>{if(!String(m).includes('Launching with arguments'))fs.appendFileSync(log,String(m)+'\n');});
 client.on('data',m=>fs.appendFileSync(log,String(m)));
 client.on('close',code=>console.log('Minecraft QA closed:',code));
 const child=await client.launch({root,authorization:Authenticator.getAuth(name),javaPath:java,version:{number:install.VERSION,type:'release',custom:install.PROFILE},memory:{max:'2G',min:'512M'},customArgs:['-Dtecni.visualTest=true','-Dtecni.expansionVisual=true'],window:{width:1280,height:720},quickPlay:{type:'multiplayer',identifier:'127.0.0.1:25573'},overrides:{detached:false}});
 fs.writeFileSync(path.join(root,'qa-season29.pid'),String(child.pid));
 console.log('Minecraft QA 2.9.0 started:',child.pid);
})().catch(e=>{console.error(e.stack);process.exitCode=1;});
