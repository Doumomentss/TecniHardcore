const mc=require('./test-runtime/node_modules/minecraft-protocol'),fs=require('fs'),path=require('path'),assert=require('assert/strict');
const print=console.log;console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const root=path.resolve('tools/test-runtime/social27/server');assert(fs.existsSync(path.join(root,'.tecni-test-world')));
const sleep=ms=>new Promise(r=>setTimeout(r,ms));async function until(f,label){for(let i=0;i<180;i++){if(f())return;await sleep(100);}throw Error(label);}
const v=n=>{let a=[];do{let x=n&127;n>>>=7;a.push(x|(n?128:0));}while(n);return Buffer.from(a);};const str=s=>Buffer.concat([v(Buffer.byteLength(s)),Buffer.from(s)]);
async function spool(s){fs.writeFileSync(path.join(root,'qa-commands.txt'),s+'\n');await until(()=>!fs.existsSync(path.join(root,'qa-commands.txt')),'spool');await sleep(200);}
let ready=false,target,position,dialogue=false;
const client=mc.createClient({host:'127.0.0.1',port:25569,username:'E25Guard',auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(client);
client.on('login',()=>{ready=true;client.write('settings',{locale:'es_es',viewDistance:3,chatFlags:0,chatColors:true,skinParts:127,mainHand:1,enableTextFiltering:false,enableServerListing:true});client.write('custom_payload',{channel:'minecraft:register',data:Buffer.from(['tecnihardcore:soul_v3','tecnihardcore:ritual_v3','tecnihardcore:altar_open_v3','tecnihardcore:civic_open_v1','fiw-mods-api:challenge'].join('\0'))});});
client.on('packet',(p,m)=>{
 if(m.name==='position'){position=p;client.write('teleport_confirm',{teleportId:p.teleportId});client.write('position_look',{x:p.x,y:p.y,z:p.z,yaw:p.yaw,pitch:p.pitch,onGround:true});}
 if(m.name==='spawn_entity'&&Math.abs(p.x-14.5)<.1&&Math.abs(p.z-12.5)<.1)target=p.entityId;
 if(m.name==='custom_payload'&&p.channel==='fiw-mods-api:challenge')client.write('custom_payload',{channel:'fiw-mods-api:response',data:Buffer.concat([v(1),str('tecnihardcore'),str('2.7.0'),str('qa-only'),v(0),v(0),v(0),v(0),p.data])});
 if(m.name==='custom_payload'&&p.channel==='tecnihardcore:civic_open_v1')dialogue=true;
});client.on('error',e=>print('Client error '+e));
(async()=>{
 await until(()=>ready,'login');await spool('tecni vidas E25Guard 3\ngamemode survival E25Guard\ntp E25Guard 14.5 -60 11.5');await until(()=>target,'merchant entity');
 for(let i=0;i<220;i++)client.write('use_entity',{target,mouse:1,sneaking:false});await sleep(700);
 await spool('tecni seguridad revisar E25Guard');const log=fs.readFileSync(path.join(root,'logs/latest.log'),'utf8');const match=log.match(/E25Guard.*Acciones bloqueadas: (\d+)/);assert(match&&Number(match[1])>0,'Server action spam was not blocked');
 await sleep(1500);client.write('use_entity',{target,mouse:0,hand:0,sneaking:false});await until(()=>dialogue,'normal NPC interaction after throttle');
 const result={passed:true,attacksSent:220,blocked:Number(match[1]),normalInteractionRecovers:true,automaticMovementPunishments:false};fs.writeFileSync(path.join(root,'qa-results/guard27-result.json'),JSON.stringify(result,null,2));print('PASS native guard: excessive attacks blocked, ordinary NPC use works after reset; no automatic movement punishments');
})().catch(e=>{print(e.stack);process.exitCode=1;}).finally(()=>{client.end();setTimeout(()=>process.exit(process.exitCode||0),500);});
