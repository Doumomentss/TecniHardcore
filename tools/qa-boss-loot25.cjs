// Exercises actual damage callbacks and final death, not combat duration/balance.
const fs=require('fs'),path=require('path'),assert=require('assert');
const output=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const mc=require('./test-runtime/node_modules/minecraft-protocol'),base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),clients=[],results=[];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn){for(let i=0;i<450;i++){if(fn())return;await sleep(200);}throw Error('Boss QA timeout');}
async function command(...lines){const p=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,lines.join('\n')+'\n');await until(()=>!fs.existsSync(p));await sleep(500);}
async function connect(name){const c=mc.createClient({host:'127.0.0.1',port:25568,username:name,auth:'offline',version:'1.20.1'});clients.push(c);require('./test-pack-handshake.cjs')(c);c.on('error',()=>{});c.on('login',()=>{c.logged=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:cataclysm_v1\0tecnihardcore:boss_fx_v1')});});c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));await until(()=>c.logged);}
function store(){return JSON.parse(fs.readFileSync(path.join(server,'world/tecnihardcore-expansion.json')));}
function check(name,ok,detail){results.push({name,ok:!!ok,detail});output((ok?'PASS':'FAIL')+': '+name);assert(ok,name);}
(async()=>{
 assert(fs.existsSync(path.join(server,'.tecni-test-world')));
 for(const name of ['E25Alpha','E25Beta','E25Low'])await connect(name);
 await command(...['E25Alpha','E25Beta','E25Low'].flatMap((name,i)=>['tecni vidas '+name+' 3','gamemode survival '+name,'effect give '+name+' minecraft:resistance 300 255 true','tp '+name+' '+(10000.5+i*3)+' 95 10000.5']));
 const before=new Set(Object.keys(store().rewards));
 await command('tecni jefe invocar 10004 95 10004');await sleep(1200);
 await command('qa25 boss-hit E25Low 20');await sleep(1000);await command('qa25 boss-hit E25Alpha 100000');await sleep(5000);
 await command('qa25 boss-hit E25Beta 200');await sleep(1000);await command('qa25 boss-hit E25Alpha 100000');await sleep(7000);
 await command('qa25 boss-hit E25Alpha 100000');await sleep(1000);
 const first=Object.entries(store().rewards).filter(([id])=>!before.has(id));
 check('Final death rewards two eligible players and excludes contribution below 5 percent',first.length===2,first);
 check('Material quantities stay within personal reward ranges',first.every(([,r])=>r.diamonds>=4&&r.diamonds<=8&&r.gold>=1&&r.gold<=3&&r.iron>=2&&r.iron<=4));
 await sleep(6500);check('Death animation ticks do not duplicate rewards',Object.keys(store().rewards).length===before.size+2);
 const next=new Set(Object.keys(store().rewards));
 await command('tecni jefe invocar 10004 95 10004','qa25 boss-hit E25Alpha 20');await sleep(1000);
 await command('kill @e[type=tecnihardcore:custodio_pizarra]');await sleep(6500);
 check('Administrative kill grants no personal loot',Object.keys(store().rewards).length===next.size);
})().catch(e=>{output(e.stack);results.push({name:'exception',ok:false,detail:e.stack});process.exitCode=1;}).finally(async()=>{clients.forEach(c=>c.end());await sleep(12000);fs.writeFileSync(path.join(base,'boss-loot-results.json'),JSON.stringify(results,null,2));process.exit(process.exitCode||0);});
