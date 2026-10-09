// Only the marked, isolated fixture on 25569. Never authenticates production users.
const mc=require('./test-runtime/node_modules/minecraft-protocol'),fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const print=console.log;console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const root=path.resolve('tools/test-runtime/social27/server');assert(fs.existsSync(path.join(root,'.tecni-test-world')));
const sleep=ms=>new Promise(r=>setTimeout(r,ms)),clients=[];
async function until(f,label,ms=12000){let end=Date.now()+ms;while(Date.now()<end){if(f())return;await sleep(100);}throw Error(label);}
const variable=n=>{let a=[];do{let x=n&127;n>>>=7;if(n)x|=128;a.push(x);}while(n);return Buffer.from(a);};
const string=s=>{let b=Buffer.from(s);return Buffer.concat([variable(b.length),b]);};
function readVar(b,o){let n=0,k=0;while(k<35){let x=b[o.i++];n|=(x&127)<<k;if(!(x&128))return n;k+=7;}throw Error('varint');}
function readStr(b,o){let n=readVar(b,o),s=b.subarray(o.i,o.i+n).toString();o.i+=n;return s;}
function uuidBytes(s){return Buffer.from(s.replaceAll('-',''),'hex');}
function ledger(){return JSON.parse(fs.readFileSync(path.join(root,'world/tecnihardcore-social.json'))).data;}
async function spool(text){fs.writeFileSync(path.join(root,'qa-commands.txt'),text+'\n');await until(()=>!fs.existsSync(path.join(root,'qa-commands.txt')),'spool consumed');await sleep(150);}
async function connect(name,reported='tecnihardcore'){
 let client=mc.createClient({host:'127.0.0.1',port:25569,username:name,auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(client);
 let b={name,client,chats:[],screens:[],connected:false};clients.push(b);
 b.command=text=>client.write('chat_command',{command:text,timestamp:BigInt(Date.now()),salt:0n,argumentSignatures:[],messageCount:0,acknowledged:Buffer.alloc(3)});
 b.chat=text=>client.write('chat_message',{message:text,timestamp:BigInt(Date.now()),salt:0n,signature:undefined,offset:0,acknowledged:Buffer.alloc(3)});
 b.action=(screen,action,id='',value='')=>client.write('custom_payload',{channel:'tecnihardcore:civic_action_v1',data:Buffer.concat([uuidBytes(screen.nonce),string(action),string(id),string(value)])});
 client.on('login',()=>{b.connected=true;client.write('custom_payload',{channel:'minecraft:register',data:Buffer.from(['tecnihardcore:pvp_guard_v1','tecnihardcore:soul_v3','tecnihardcore:ritual_v3','tecnihardcore:altar_open_v3','tecnihardcore:relic_v2','tecnihardcore:civic_open_v1','tecnihardcore:npc_skin_data_v1','fiw-mods-api:challenge'].join('\0'))});});
 client.on('packet',(p,m)=>{
  if(m.name==='update_health'&&p.health<=0)setTimeout(()=>client.write('client_command',{actionId:0}),150);
  if(m.name==='position'){b.position=p;client.write('teleport_confirm',{teleportId:p.teleportId});}
  if(m.name==='system_chat'||m.name==='player_chat')b.chats.push(JSON.stringify(p));
  if(m.name==='custom_payload'&&Buffer.isBuffer(p.data)){
   if(p.channel==='tecnihardcore:civic_open_v1')b.screens.push(JSON.parse(readStr(p.data,{i:0})));
   if(p.channel==='fiw-mods-api:challenge'){
    let data=Buffer.concat([variable(1),string(reported),string('2.7.0'),string('qa-only'),variable(0),variable(0),variable(0),variable(0),p.data]);client.write('custom_payload',{channel:'fiw-mods-api:response',data});
   }
  }
 });
 client.on('error',e=>{b.error=String(e);});client.on('kick_disconnect',p=>{b.kicked=JSON.stringify(p);});client.on('end',()=>b.ended=true);
 await until(()=>b.connected,'connect '+name);await sleep(1300);b.id=client.uuid;return b;
}
async function command(b,text){b.command(text);await sleep(300);}
async function screen(b,commandText){let n=b.screens.length;await command(b,commandText);await until(()=>b.screens.length>n,'open screen '+commandText);return b.screens.at(-1);}
async function choice(b,npc,index){let n=b.screens.length;await spool('qa25 civic '+b.name+' '+npc);await until(()=>b.screens.length>n,'NPC dialog');let current=b.screens.at(-1);b.action(current,'choice',String(index));await sleep(350);}
async function report(b,tag){await spool('qa25 social-report '+b.name+' '+tag);return JSON.parse(fs.readFileSync(path.join(root,'qa-results',b.name+'-'+tag+'.json')));}

const moderation=()=>JSON.parse(fs.readFileSync(path.join(root,'world/tecnihardcore-moderacion.json')));
async function test(){
 const a=await connect('E25GuardI'),b=await connect('E25GuardJ');
 await spool('qa25 social-reset E25GuardI\ngamerule doMobSpawning false\ntecni vidas E25GuardI 5\ntecni vidas E25GuardJ 5\ngamemode survival @a\nclear @a\neffect clear @a\ntp E25GuardI 40 -60 40\ntp E25GuardJ 42 -60 40');
 await sleep(2200); await spool('damage E25GuardJ 100 minecraft:player_attack by E25GuardI');await sleep(1500);
 let data=moderation();assert(data.protection[b.id]>1790000);assert(Object.values(data.clips).some(c=>c.player===b.id&&c.killer===a.id));print('PASS PvP death creates 30-minute guard and death marker');
 await spool('effect clear E25GuardJ\ndamage E25GuardJ 4 minecraft:player_attack by E25GuardI');assert.equal((await report(b,'guard-in')).health,20);
 await spool('damage E25GuardI 4 minecraft:player_attack by E25GuardJ');assert.equal((await report(a,'guard-out')).health,20);
 await spool('damage E25GuardJ 4 minecraft:arrow by E25GuardI');assert.equal((await report(b,'arrow')).health,20);print('PASS mutual PvP and projectile blocking');
 await spool('damage E25GuardJ 4 minecraft:generic');let worldHealth=(await report(b,'world')).health;assert(worldHealth<20&&worldHealth>0);await sleep(700);
 await spool('summon minecraft:zombie 45 -60 40 {NoAI:1b,Tags:["qa28mob"]}\ndamage E25GuardJ 4 minecraft:mob_attack by @e[tag=qa28mob,limit=1]');assert((await report(b,'mob')).health<worldHealth);print('PASS world and mob damage still enabled');
 await command(b,'tecni replays listar');assert(!b.chats.some(t=>t.includes('· E25GuardJ ·')));print('PASS non-operator denied private recordings');
 await sleep(16000);await spool('save-all flush');data=moderation();let clip=Object.values(data.clips).filter(c=>c.player===b.id).at(-1);assert.equal(clip.status,'lista');assert(clip.segments.length);assert(clip.markerMillis>0);print('PASS saved death replay '+clip.segments.at(-1));
 b.client.end();await until(()=>b.ended,'disconnect');await sleep(500);let remain=moderation().protection[b.id];await sleep(2400);await spool('save-all flush');assert.equal(moderation().protection[b.id],remain);let re=await connect('E25GuardJ');await sleep(2200);await spool('save-all flush');assert(moderation().protection[b.id]<remain);print('PASS offline pauses, reconnect resumes');
 await spool('kill E25GuardI');await sleep(17500);await spool('save-all flush');data=moderation();assert(Object.values(data.clips).some(c=>c.player===a.id&&c.status==='lista'));assert(!data.protection[a.id]);print('PASS /kill also recorded, without PvP protection');
 fs.writeFileSync(path.resolve('tools/test-runtime/moderation28/server-results.json'),JSON.stringify({passed:true,time:new Date().toISOString(),tests:['pvp-death','mutual-guard','projectile','world-damage','mob-damage','permissions','saved-replay','offline-pause','reconnect','kill-replay'],data},null,2));
}
test().catch(e=>{print('FAIL '+e.stack);process.exitCode=1;}).finally(()=>{for(let b of clients)b.client.end();setTimeout(()=>process.exit(process.exitCode||0),800);});
