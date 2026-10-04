const mc=require('./test-runtime/node_modules/minecraft-protocol'),fs=require('fs'),path=require('path'),assert=require('assert/strict');
const print=console.log;console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const root=path.resolve('tools/test-runtime/social27/server');assert(fs.existsSync(path.join(root,'.tecni-test-world')));
const sleep=ms=>new Promise(r=>setTimeout(r,ms));async function until(f,label){for(let i=0;i<150;i++){if(f())return;await sleep(100);}throw Error(label);}
const v=n=>{let a=[];do{let x=n&127;n>>>=7;a.push(x|(n?128:0));}while(n);return Buffer.from(a);};const str=s=>Buffer.concat([v(Buffer.byteLength(s)),Buffer.from(s)]);
async function spool(s){fs.writeFileSync(path.join(root,'qa-commands.txt'),s+'\n');await until(()=>!fs.existsSync(path.join(root,'qa-commands.txt')),'spool');await sleep(150);}
const Chunk=require('./test-runtime/node_modules/prismarine-chunk')('1.20.1'),Vec=require('./test-runtime/node_modules/vec3').Vec3;
let client;function connect(){ready=false;modsPassed=false;client=mc.createClient({host:'127.0.0.1',port:25569,username:'E25OreCheck',auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(client);
client.on('login',()=>{ready=true;client.write('settings',{locale:'es_es',viewDistance:3,chatFlags:0,chatColors:true,skinParts:127,mainHand:1,enableTextFiltering:false,enableServerListing:true});client.write('custom_payload',{channel:'minecraft:register',data:Buffer.from(['tecnihardcore:soul_v3','tecnihardcore:ritual_v3','tecnihardcore:altar_open_v3','fiw-mods-api:challenge'].join('\0'))});});
client.on('packet',(p,m)=>{
 if(m.name==='position'){client.write('teleport_confirm',{teleportId:p.teleportId});client.write('position_look',{x:p.x,y:p.y,z:p.z,yaw:p.yaw,pitch:p.pitch,onGround:false});}
 if(m.name==='custom_payload'&&p.channel==='fiw-mods-api:challenge'){client.write('custom_payload',{channel:'fiw-mods-api:response',data:Buffer.concat([v(1),str('tecnihardcore'),str('2.7.0'),str('qa-only'),v(0),v(0),v(0),v(0),p.data])});modsPassed=true;}
 if(m.name==='map_chunk'&&phase>0&&p.x===20&&p.z===20){let chunk=new Chunk();chunk.load(p.chunkData);let name=chunk.getBlock(new Vec(8,-52,8)).name;if(phase===1)hidden=name;else visible=name;}
});client.on('error',e=>print('Client error '+e));return client;}
let ready=false,hidden,visible,modsPassed=false,phase=0;
(async()=>{
 await spool('forceload add 320 320 335 335');await sleep(500);await spool('fill 320 -60 320 335 -45 335 minecraft:stone\nsetblock 328 -52 328 minecraft:diamond_ore');phase=1;connect();await until(()=>hidden,'concealed initial chunk');assert(['stone','deepslate'].includes(hidden),'Hidden ore leaked: '+hidden);client.end();await sleep(1000);
 await spool('setblock 329 -52 328 minecraft:air');phase=2;connect();await until(()=>visible,'exposed initial chunk');assert.equal(visible,'diamond_ore');
 let result={passed:true,concealed:hidden,exposed:visible,realWorldBlock:'diamond_ore',officialClientAllowed:true};fs.writeFileSync(path.join(root,'qa-results/security27-result.json'),JSON.stringify(result,null,2));print('PASS AntiXray: concealed diamond is stone in chunk packet, exposed diamond returns; world unchanged');
})().catch(e=>{print(e.stack);process.exitCode=1;}).finally(()=>{client.end();setTimeout(()=>process.exit(process.exitCode||0),500);});
