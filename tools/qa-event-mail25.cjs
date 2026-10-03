// Real event lifecycle and reward delivery on the marked isolated world only.
const fs=require('fs'),path=require('path'),assert=require('assert');
const output=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const mc=require('./test-runtime/node_modules/minecraft-protocol');
const base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),results=process.argv.includes('--complete')?JSON.parse(fs.readFileSync(path.join(base,'event-mail-results.json'))).filter(r=>r.ok===true):[],clients=[];
assert(fs.existsSync(path.join(server,'.tecni-test-world')));
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,ms=90000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(200);}throw Error('Test condition timeout');}
async function command(...lines){const p=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,lines.join('\n')+'\n');await until(()=>!fs.existsSync(p));await sleep(400);}
async function report(name,tag){await command(`qa25 report ${name} ${tag}`);return JSON.parse(fs.readFileSync(path.join(server,'qa-results',`${name}-${tag}.json`)));}
function check(name,ok,detail){results.push({name,ok:!!ok,detail});output((ok?'PASS':'FAIL')+': '+name);assert(ok,name);}
async function connect(name){const c=mc.createClient({host:'127.0.0.1',port:25568,username:name,auth:'offline',version:'1.20.1'});clients.push(c);require('./test-pack-handshake.cjs')(c);c.on('error',e=>output(e.message));c.on('login',()=>{c.logged=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:cataclysm_v1\0tecnihardcore:event_v1')});});c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));await until(()=>c.logged);return c;}
function state(){return JSON.parse(fs.readFileSync(path.join(server,'world/tecnihardcore-expansion.json')));}
async function observe(type,mode,joined){await command('execute in tecnihardcore:eventos run tp E25View '+(joined.x+25)+' 95 '+(joined.z+25)+' 135 20');await sleep(1500);const p=path.join(base,'game/qa-capture-request.txt');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,'event-'+type+'-'+mode);await until(()=>!fs.existsSync(p));}
async function prepare(type,mode){const offset=fs.statSync(path.join(server,'logs/latest.log')).size;await command(`execute as E25Alpha run tecni evento preparar ${type} ${mode}`);let id;await until(()=>{const text=fs.readFileSync(path.join(server,'logs/latest.log')).subarray(offset).toString();id=text.match(/Event arena ready: ([a-f0-9-]{36})/)?.[1];return !!id;});return id;}
async function stopped(id){await until(()=>fs.existsSync(path.join(server,'world/tecni-event-recovery/results',id+'.json')));await sleep(1500);return JSON.parse(fs.readFileSync(path.join(server,'world/tecni-event-recovery/results',id+'.json')));}
(async()=>{
 await connect('E25Alpha');await connect('E25Beta');
 await command('op E25Alpha','op E25Beta','tecni desastre detener todos','tecni vidas E25Alpha 3','tecni vidas E25Beta 3','gamemode survival E25Alpha','gamemode survival E25Beta','effect clear E25Alpha','effect clear E25Beta','tp E25Alpha 10000.5 95 10000.5','tp E25Beta 10006.5 95 10000.5','clear E25Alpha','clear E25Beta','give E25Alpha minecraft:emerald 7','give E25Beta minecraft:lapis_lazuli 9');
 // Every preset/mode must enroll, stop without prizes, and preserve actual inventory/lives.
 for(const type of (process.argv.includes('--complete')?[]:['tormenta','circuito','defensa']))for(const mode of ['ensayo','hardcore']){
  const before=await report('E25Alpha',`${type}-${mode}-before`),id=await prepare(type,mode);
  await command(`execute as E25Alpha run tecni evento entrar ${id}`);
  const joined=await report('E25Alpha',`${type}-${mode}-joined`);check(type+' '+mode+' enters dedicated arena',joined.dimension==='tecnihardcore:eventos');
  await command(`tecni evento iniciar ${id}`);await sleep(11000);await observe(type,mode,joined);
  if(mode==='ensayo')await command('qa25 lethal E25Alpha');
  await command(`tecni evento detener ${id}`);const outcome=await stopped(id),after=await report('E25Alpha',`${type}-${mode}-after`);
  check(type+' '+mode+' cancellation preserves inventory and lives',before.inventory===after.inventory&&before.lives===after.lives,{before,after});
  check(type+' '+mode+' cancelled event gives no reward',outcome.status==='cancelado'&&!Object.keys(state().rewards).some(k=>k.startsWith('event:'+id+':')));
 }
 // Actual five-wave spawns/death callbacks; /kill is a QA stimulus, not a combat balance test.
 for(const mode of ['ensayo','hardcore']){
  const id=await prepare('defensa',mode);await command(`execute as E25Alpha run tecni evento entrar ${id}`,`tecni evento iniciar ${id}`);await sleep(11000);
  for(let wave=1;wave<=5;wave++){await sleep(5200);await command(`execute in tecnihardcore:eventos run kill @e[nbt={Tags:["TecniEvent:${id}"]},type=!tecnihardcore:bestia_cristal]`);}
  const result=await stopped(id);check('Defense '+mode+' completes all five waves',result.status==='completo',result);
  const mail=Object.entries(state().rewards).filter(([k])=>k.startsWith('event:'+id+':'));
  check('Defense '+mode+' reward follows announced mode',mode==='ensayo'?mail.length===0:mail.length===1&&mail[0][1].diamonds===4);
 }
 // Race uses server teleports of temporary mounts to exercise checkpoint order, not flying skill.
 for(const mode of ['ensayo','hardcore']){
  const before=await report('E25Alpha','race-'+mode+'-before'),id=await prepare('circuito',mode);
  await command(`execute as E25Alpha run tecni evento entrar ${id}`,`tecni evento iniciar ${id}`);await sleep(11000);
  const rider=await report('E25Alpha','race-'+mode+'-rider'),cx=Math.round(rider.x-8.5);
  for(let lap=0;lap<3;lap++)for(let i=0;i<12;i++){
   const a=i*Math.PI/6,x=cx+.5+Math.cos(a)*40,y=65+8+(i%3)*8,z=.5+Math.sin(a)*40;
   await command(`execute in tecnihardcore:eventos run tp @e[type=tecnihardcore:bestia_cristal,nbt={Tags:["TecniEvent:${id}"]}] ${x} ${y} ${z}`);
  }
  const result=await stopped(id),after=await report('E25Alpha','race-'+mode+'-after');
  check('Circuit '+mode+' accepts three ordered laps',result.status==='completo'&&result.participants[0].laps===3,result);
  check('Circuit '+mode+' temporary mount and equipment cannot leave',after.activeEntities===0&&before.inventory===after.inventory);
  const mail=Object.entries(state().rewards).filter(([k])=>k.startsWith('event:'+id+':'));
  check('Circuit '+mode+' correct personal reward',mode==='ensayo'?mail.length===0:mail.length===1&&mail[0][1].diamonds===6);
 }
 // Persistent mailbox: full inventory, first delivery, repeated claim.
 await command('clear E25Beta',...[...Array(36)].map((_,i)=>`item replace entity E25Beta ${i<9?'hotbar.'+i:'inventory.'+(i-9)} with minecraft:cobblestone 64`));
 const stamp=String(Date.now());await command(`qa25 reward E25Beta ${stamp}`,'execute as E25Beta run tecni recompensas');
 let pair=Object.entries(state().rewards).find(([k])=>k.startsWith('qa:'+stamp+':'));check('Full inventory retains pending reward',pair&&pair[1].state==='pending');
 await command('clear E25Beta','execute as E25Beta run tecni recompensas');const once=await report('E25Beta','mail-once');
 check('Reward delivery commits claimed state',state().rewards[pair[0]].state==='claimed');
 await command('execute as E25Beta run tecni recompensas');const twice=await report('E25Beta','mail-twice');check('Repeated claim does not duplicate inventory',once.inventory===twice.inventory);
})().catch(e=>{output(e.stack);results.push({name:'exception',ok:false,detail:e.stack});process.exitCode=1;}).finally(async()=>{clients.forEach(c=>c.end());await sleep(12000);fs.writeFileSync(path.join(base,'event-mail-results.json'),JSON.stringify(results,null,2));process.exit(process.exitCode||0);});
