// Real ten-minute event clocks, both modes, with a protected scripted participant.
// This verifies lifecycle/persistence/rewards, not natural combat balance.
const fs=require('fs'),path=require('path'),assert=require('assert');
const mc=require('./test-runtime/node_modules/minecraft-protocol'),base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),results=[];
const out=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,ms=90000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(250);}throw Error('Storm lifecycle timeout');}
async function command(...lines){const p=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,lines.join('\n')+'\n');await until(()=>!fs.existsSync(p));await sleep(400);}
async function report(tag){await command('qa25 report E25Storm '+tag);return JSON.parse(fs.readFileSync(path.join(server,'qa-results','E25Storm-'+tag+'.json')));}
function store(){return JSON.parse(fs.readFileSync(path.join(server,'world/tecnihardcore-expansion.json')));}
function check(name,ok,detail){results.push({name,ok:!!ok,detail});out((ok?'PASS':'FAIL')+': '+name);assert(ok,name);}
let c,timer;
(async()=>{
 assert(fs.existsSync(path.join(server,'.tecni-test-world')));c=mc.createClient({host:'127.0.0.1',port:25568,username:'E25Storm',auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(c);c.on('error',()=>{});c.on('login',()=>{c.logged=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:cataclysm_v1\0tecnihardcore:event_v1')});});c.on('position',p=>{c.pos=p;c.write('teleport_confirm',{teleportId:p.teleportId});});await until(()=>c.logged);
 timer=setInterval(()=>{if(c.state==='play'&&c.pos)c.write('position',{x:c.pos.x,y:c.pos.y,z:c.pos.z,onGround:true});},500);
 await command('op E25Storm','tecni vidas E25Storm 3','gamemode survival E25Storm','tp E25Storm 10000.5 95 10000.5','clear E25Storm','give E25Storm emerald 17');
 for(const mode of ['ensayo','hardcore']){
  const before=await report(mode+'-before'),offset=fs.statSync(path.join(server,'logs/latest.log')).size;let id;
  await command('execute as E25Storm run tecni evento preparar tormenta '+mode);await until(()=>{id=fs.readFileSync(path.join(server,'logs/latest.log')).subarray(offset).toString().match(/Event arena ready: ([a-f0-9-]{36})/)?.[1];return id;});
  await command(`execute as E25Storm run tecni evento entrar ${id}`,'effect give E25Storm resistance 720 255 true',`tecni evento iniciar ${id}`);
  const began=Date.now();for(let stage=0;stage<3;stage++){await sleep(stage===0?12000:200000);out(mode+' stage '+(stage+1)+' / elapsed '+Math.round((Date.now()-began)/1000)+'s');assert(c.state==='play');}
  const file=path.join(server,'world/tecni-event-recovery/results',id+'.json');await until(()=>fs.existsSync(file),230000);const outcome=JSON.parse(fs.readFileSync(file));await sleep(1500);
  check('Storm '+mode+' completes its real 600-second clock',outcome.status==='completo'&&outcome.seconds>=600,outcome);
  const mail=Object.entries(store().rewards).filter(([key])=>key.startsWith('event:'+id+':'));check('Storm '+mode+' correct personal rewards',mode==='ensayo'?mail.length===0:mail.length===1&&mail[0][1].diamonds===4&&!!mail[0][1].trophy);
  const after=await report(mode+'-after');check('Storm '+mode+' returns inventory and lives',after.inventory===before.inventory&&after.lives===before.lives&&after.dimension===before.dimension,{before,after});
 }
})().catch(e=>{out(e.stack);results.push({name:'exception',ok:false,detail:e.stack});process.exitCode=1;}).finally(async()=>{clearInterval(timer);c?.end();await sleep(12000);fs.writeFileSync(path.join(base,'storm-event-results.json'),JSON.stringify(results,null,2));process.exit(process.exitCode||0);});
