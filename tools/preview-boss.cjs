// Local-only world and client. It never reads or changes production player data.
const fs=require('fs'),path=require('path'),net=require('net'),{spawn}=require('child_process');
const {Client,Authenticator}=require('../launcher/node_modules/minecraft-launcher-core');
const install=require('../launcher/client-install');
const root=path.resolve(__dirname,'..'),base=path.join(root,'tools/test-runtime/boss-preview'),server=path.join(base,'server'),game=path.join(base,'game');
async function listening(){return new Promise(r=>{const s=net.connect(25567,'127.0.0.1');s.once('connect',()=>{s.destroy();r(true)});s.once('error',()=>r(false));});}
(async()=>{
 fs.mkdirSync(server,{recursive:true});fs.mkdirSync(game,{recursive:true});
 for(const name of ['libraries','fabric-server-launch.jar','server.jar','fabric-server-launcher.properties','.fabric']){const src=path.join(root,'server',name);if(fs.existsSync(src)&&!fs.existsSync(path.join(server,name)))fs.cpSync(src,path.join(server,name),{recursive:true});}
 fs.mkdirSync(path.join(server,'mods'),{recursive:true});
 for(const name of fs.readdirSync(path.join(root,'server/mods'))){if(/^(easyauth|ledger|tecnihardcore|luckperms|graves)/i.test(name))continue;fs.copyFileSync(path.join(root,'server/mods',name),path.join(server,'mods',name));}
 const extra=path.join(root,'tools/test-runtime/content24/mods');
 if(fs.existsSync(extra))for(const name of fs.readdirSync(extra))fs.copyFileSync(path.join(extra,name),path.join(server,'mods',name));
 const jar=path.join(root,'custom_mods/hardcore/build/libs/tecnihardcore-2.4.0.jar');if(!fs.existsSync(jar))throw Error('Falta el mod de pruebas 2.4.0.');
 for(const name of fs.readdirSync(path.join(server,'mods')))if(/^(graves|ledger|easyauth|tecnihardcore)-.*\.jar$/i.test(name))fs.unlinkSync(path.join(server,'mods',name));
 fs.copyFileSync(jar,path.join(server,'mods/tecnihardcore-2.4.0.jar'));fs.writeFileSync(path.join(server,'eula.txt'),'eula=true\n');
 fs.writeFileSync(path.join(server,'server.properties'),'server-ip=127.0.0.1\nserver-port=25567\nonline-mode=false\nlevel-name=arena\nlevel-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\ndifficulty=hard\nspawn-protection=0\nview-distance=8\nsimulation-distance=6\nmax-players=4\n');
 fs.mkdirSync(path.join(server,'config/voicechat'),{recursive:true});fs.writeFileSync(path.join(server,'config/voicechat/voicechat-server.properties'),'port=25568\nbind_address=127.0.0.1\n');
 const java=await install.ensureJava(game,null,console.log);
 if(!await listening()){
  const out=fs.openSync(path.join(server,'preview-console.log'),'a');const process=spawn(java,['-Xms512M','-Xmx2G','-Dtecni.testServer=true','-Dtecni.bossPreview=true','-jar','fabric-server-launch.jar','nogui'],{cwd:server,windowsHide:true,stdio:['pipe',out,out]});
  fs.writeFileSync(path.join(base,'server.pid'),String(process.pid));process.on('error',e=>console.error(e.message));
  process.on('exit',code=>console.log('Arena cerrada: '+code));process.stdin.on('error',()=>{});
  global.previewServer=process;
  for(let i=0;i<90&&!await listening();i++)await new Promise(r=>setTimeout(r,1000));if(!await listening())throw Error('La arena no arrancó. Revisa '+path.join(server,'preview-console.log'));
 }
 process.stdin.on("data",data=>{if(global.previewServer)global.previewServer.stdin.write(data);});
 const pack=path.join(root,'installer_payload');await install.prepareClient(game,pack,console.log,{host:'127.0.0.1',port:25567});
 if(process.env.TECNI_GRAPHICS_QA)await require('../launcher/graphics').apply(game,process.env.TECNI_GRAPHICS_QA,pack,console.log);
 for(const name of fs.readdirSync(path.join(game,'mods')))if(/^tecnihardcore-.*\.jar$/.test(name))fs.unlinkSync(path.join(game,'mods',name));fs.copyFileSync(jar,path.join(game,'mods/tecnihardcore-2.4.0.jar'));
 const client=new Client();client.on('debug',m=>{if(!String(m).includes('Launching with arguments'))fs.appendFileSync(path.join(base,'client.log'),String(m)+'\n')});client.on('data',m=>fs.appendFileSync(path.join(base,'client.log'),String(m)));client.on('close',()=>{process.stdin.pause();if(global.previewServer)global.previewServer.stdin.end('stop\n');});
 const child=await client.launch({root:game,authorization:Authenticator.getAuth('ProbadorBoss'),javaPath:java,version:{number:install.VERSION,type:'release',custom:install.PROFILE},memory:{max:'2G',min:'512M'},customArgs:process.env.TECNI_BOSS_QA?['-Dtecni.visualTest=true','-Dtecni.bossVisual=true']:[],window:{width:1280,height:720},quickPlay:{type:'multiplayer',identifier:'127.0.0.1:25567'},overrides:{detached:false}});
 fs.writeFileSync(path.join(base,'client.pid'),String(child.pid));console.log('Prueba aislada abierta. Cierra este Minecraft para detener la arena.');
})().catch(e=>{console.error(e.message);if(global.previewServer)global.previewServer.stdin.end('stop\n');process.exitCode=1;});
