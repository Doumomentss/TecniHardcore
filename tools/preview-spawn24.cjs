// Native visual QA against the restored world copy on localhost:25566.
const path=require('path'),fs=require('fs'),install=require('../launcher/client-install');
const {Client,Authenticator}=require('../launcher/node_modules/minecraft-launcher-core');
(async()=>{
 const root=path.resolve('tools/test-runtime/boss-preview/game'),pack=path.resolve('installer_payload');
 const java=await install.ensureJava(root,null,console.log);await install.prepareClient(root,pack,console.log,{host:'127.0.0.1',port:25566});
 const launcher=new Client(),log=path.join(root,'logs/spawn24-visual.log');fs.writeFileSync(log,'');
 launcher.on('debug',m=>{if(!String(m).includes('Launching with arguments'))fs.appendFileSync(log,String(m)+'\n');});launcher.on('data',m=>fs.appendFileSync(log,m));launcher.on('close',c=>console.log('Visual client exited: '+c));
 const child=await launcher.launch({root,authorization:Authenticator.getAuth('TecniSoulVisual'),javaPath:java,version:{number:install.VERSION,type:'release',custom:install.PROFILE},memory:{max:'2G',min:'512M'},customArgs:['-Dtecni.visualTest=true','-Dtecni.bossVisual=true'],window:{width:1280,height:720},quickPlay:{type:'multiplayer',identifier:'127.0.0.1:25566'},overrides:{detached:false}});
 fs.writeFileSync(path.join(root,'spawn24-test.pid'),String(child.pid));console.log('Native new content/spawn test PID '+child.pid);
})().catch(e=>{console.error(e);process.exitCode=1;});
