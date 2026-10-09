// Separate client. Only immutable Minecraft runtime caches are copied from an earlier fixture.
const fs=require('fs'),path=require('path'),{Client,Authenticator}=require('../launcher/node_modules/minecraft-launcher-core');
const install=require('../launcher/client-install');
const root=path.resolve(__dirname,'..'),base=path.join(root,'tools/test-runtime/expansion25'),game=path.join(base,'game');
const port=Number(process.env.TECNI_QA_PORT||25569);if(![25568,25569].includes(port))throw Error('Only isolated QA ports allowed');
(async()=>{
 fs.mkdirSync(game,{recursive:true});
 const cache=path.join(root,'tools/test-runtime/boss-preview/game');
 for(const dir of ['assets','libraries','versions','runtime'])if(!fs.existsSync(path.join(game,dir))&&fs.existsSync(path.join(cache,dir)))fs.cpSync(path.join(cache,dir),path.join(game,dir),{recursive:true});
 await install.prepareClient(game,path.join(root,'installer_payload'),console.log,{host:'127.0.0.1',port});
 // Opt-in preview uses the freshly built mod without changing the distributed pack.
 const qaJar=path.join(root,'custom_mods/hardcore/build/libs/tecnihardcore-'+require('../launcher/package.json').version+'.jar');
 if(process.env.TECNI_QA_PATCH==='1')fs.copyFileSync(qaJar,path.join(game,'mods',path.basename(qaJar)));
 await require('../launcher/graphics').apply(game,'optimized',path.join(root,'installer_payload'),console.log);
 const java=await install.ensureJava(game,null,console.log),client=new Client(),log=path.join(base,'client-social27.log');
 client.on('debug',m=>{if(!String(m).includes('Launching with arguments'))fs.appendFileSync(log,String(m)+'\n');});
 client.on('data',m=>fs.appendFileSync(log,String(m)));client.on('close',code=>{console.log('QA Minecraft closed: '+code);process.exitCode=code===0?0:1;});
 const child=await client.launch({root:game,authorization:Authenticator.getAuth('E25CivicA'),javaPath:java,version:{number:install.VERSION,type:'release',custom:install.PROFILE},memory:{max:'2G',min:'512M'},customArgs:['-Dtecni.visualTest=true','-Dtecni.expansionVisual=true'],window:{width:1280,height:720},quickPlay:{type:'multiplayer',identifier:'127.0.0.1:'+port},overrides:{detached:false}});
 fs.writeFileSync(path.join(base,'client.pid'),String(child.pid));console.log('Isolated Minecraft opened: '+child.pid);
})().catch(error=>{console.error(error.stack);process.exitCode=1;});
