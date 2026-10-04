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
 client.on('login',()=>{b.connected=true;client.write('custom_payload',{channel:'minecraft:register',data:Buffer.from(['tecnihardcore:soul_v3','tecnihardcore:ritual_v3','tecnihardcore:altar_open_v3','tecnihardcore:relic_v2','tecnihardcore:civic_open_v1','tecnihardcore:npc_skin_data_v1','fiw-mods-api:challenge'].join('\0'))});});
 client.on('packet',(p,m)=>{
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
async function test(){
 const a=await connect('E25CivicA'),b=await connect('E25CivicB'),c=await connect('E25CivicC');
 await spool('qa25 social-reset E25CivicA\ngamerule doMobSpawning false\ngamerule doDaylightCycle false\ntime set day\nsetworldspawn 0 -60 0\ntecni vidas E25CivicA 3\ntecni vidas E25CivicB 3\ntecni vidas E25CivicC 3\ngamemode survival @a\nclear @a\ntp E25CivicA 14.5 -60 -10.5\ntp E25CivicB 14.5 -60 11.5\ntp E25CivicC 15.5 -60 12.5');
 await command(a,'team crear Cristales');await command(a,'team invitar E25CivicB');await command(b,'team aceptar');assert.equal(Object.values(ledger().teams)[0].members.length,2);
 await command(b,'team');b.chat('PRIVATE_CIVIC_27');await sleep(600);assert(a.chats.some(t=>t.includes('PRIVATE_CIVIC_27')));assert(b.chats.some(t=>t.includes('PRIVATE_CIVIC_27')));assert(!c.chats.some(t=>t.includes('PRIVATE_CIVIC_27')));
 await command(b,'chat');b.chat('PUBLIC_CIVIC_27');await sleep(500);assert(c.chats.some(t=>t.includes('PUBLIC_CIVIC_27')));print('PASS teams: non-OP commands and private/general channels');
 await spool('effect clear E25CivicB\ndamage E25CivicB 4 minecraft:player_attack by E25CivicA');assert.equal((await report(b,'friendly')).health,20);
 b.client.end();await until(()=>b.ended,'B disconnect');await command(a,'team expulsar E25CivicB');let truce=ledger().truces.find(t=>t.expelled===b.id);assert(truce);let remaining=truce.remaining;await sleep(2400);await spool('save-all flush');assert.equal(ledger().truces[0].remaining,remaining);
 let re=await connect('E25CivicB');await sleep(1800);assert(re.chats.some(t=>t.includes('Has salido o sido expulsado')));await spool('damage E25CivicB 4 minecraft:player_attack by E25CivicA');assert.equal((await report(re,'truce')).health,20);print('PASS friendly fire and offline kick, notification, paused two-hour protection');
 await spool('tp E25CivicA 14.5 -60 -10.5\ngive E25CivicA minecraft:oak_log 16');await choice(a,'bruno',0);await choice(a,'bruno',1);assert.equal(ledger().wallets[a.id],25);assert(!(await report(a,'quest')).inventory.includes('minecraft:oak_log'));await choice(a,'bruno',1);assert.equal(ledger().wallets[a.id],25);print('PASS NPC quest consumes exact materials, pays own currency once');
 await spool('tp E25CivicA 14.5 -60 11.5\ntp E25CivicB 15.5 -60 12.5\ntp E25CivicC 14.5 -60 13.5\ntecni moneda dar E25CivicB 100\ntecni moneda dar E25CivicC 100\ngive E25CivicA minecraft:diamond 3');
 let m=await screen(a,'mercado');a.action(m,'publish','','20');await sleep(500);let offer=Object.values(ledger().offers).find(o=>o.state==='open');assert(offer);assert(!(await report(a,'escrow')).inventory.includes('minecraft:diamond'));
 let sb=await screen(re,'mercado'),sc=await screen(c,'mercado');re.action(sb,'buy',offer.id);c.action(sc,'buy',offer.id);await sleep(500);let data=ledger();assert.equal(data.offers[offer.id].state,'sold');assert.equal(data.wallets[a.id],45);assert.equal(Object.values(data.parcels).length,1);let buyer=data.wallets[re.id]===80?re:c;assert.equal(data.wallets[re.id]+data.wallets[c.id],180);print('PASS escrow and simultaneous buyers: one sale, one parcel, conserved currency');
 await spool('give '+buyer.name+' minecraft:cobblestone 2304');let parcel=Object.values(ledger().parcels)[0];let mailbox=await screen(buyer,'mercado');buyer.action(mailbox,'tab','mail');await sleep(350);mailbox=buyer.screens.at(-1);buyer.action(mailbox,'withdraw',parcel.id);await sleep(350);assert(ledger().parcels[parcel.id]);
 await spool('clear '+buyer.name);mailbox=await screen(buyer,'mercado');buyer.action(mailbox,'tab','mail');await sleep(350);mailbox=buyer.screens.at(-1);buyer.action(mailbox,'withdraw',parcel.id);buyer.action(mailbox,'withdraw',parcel.id);await sleep(350);assert(!ledger().parcels[parcel.id]);let inv=(await report(buyer,'withdraw')).inventory;assert(inv.includes('minecraft:diamond'));assert(inv.includes('Count:3b'));print('PASS full inventory preserves parcel; replayed withdrawal cannot duplicate');
 await spool('give E25CivicA minecraft:iron_ingot 8');m=await screen(a,'mercado');a.action(m,'publish','','10');await sleep(350);offer=Object.values(ledger().offers).find(o=>o.state==='open');m=a.screens.at(-1);a.action(m,'cancel',offer.id);await sleep(350);assert.equal(ledger().offers[offer.id].state,'cancelled');assert(Object.values(ledger().parcels).some(p=>p.owner===a.id&&p.item.includes('minecraft:iron_ingot')));print('PASS cancellation returns escrow to private mailbox');
 await spool('gamemode creative E25CivicA');await command(a,'mercado vender 1');assert(a.chats.some(t=>t.includes('Debes estar vivo')));await spool('gamemode survival E25CivicA');
 let before=re.screens.length;await command(re,'tecni npc editar selma');await sleep(250);assert.equal(re.screens.length,before);print('PASS non-OP editor denied, creative marketplace rejected');
 const bad=await connect('E25BadMod','meteor-client');await until(()=>bad.kicked||bad.ended,'cheat mod rejected',8000);assert(bad.kicked?.includes('Cliente no permitido'));print('PASS Fiw nonce challenge rejects known cheat report');
 await spool('save-all flush');fs.writeFileSync(path.join(root,'qa-results/social27-result.json'),JSON.stringify({passed:true,time:new Date().toISOString(),tests:['teams','private-chat','friendly-fire','offline-truce','quests','escrow','concurrent-buy','full-inventory','nonce-replay','cancel','permissions','fiw-known-cheat'],data:ledger()},null,2));
}
test().catch(e=>{print('FAIL '+e.stack);process.exitCode=1;}).finally(()=>{for(let b of clients)b.client.end();setTimeout(()=>process.exit(process.exitCode||0),800);});
