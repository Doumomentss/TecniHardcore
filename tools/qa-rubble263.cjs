const fs=require('fs'),path=require('path'),assert=require('assert'),mc=require('./test-runtime/node_modules/minecraft-protocol');
const base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),game=path.join(base,'game'),out=path.resolve('tools/test-runtime/rubble263');
assert(fs.existsSync(path.join(server,'.tecni-test-world')));fs.mkdirSync(out,{recursive:true});const rows=[],sleep=ms=>new Promise(r=>setTimeout(r,ms));let c,packets=0,samples=0,removed=0,bytes=0,breakEvents=0,active=false;
async function until(f,ms=120000){const end=Date.now()+ms;while(Date.now()<end){if(f())return;await sleep(150);}throw Error('timeout');}
const log=()=>fs.readFileSync(path.join(server,'logs/latest.log'),'utf8');
async function command(...lines){const f=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,lines.join('\n')+'\n');await until(()=>!fs.existsSync(f));await sleep(300);}
async function capture(tag){await until(()=>log().includes('E25View joined the game'));await command('gamemode spectator E25View','execute in tecnihardcore:eventos run tp E25View 39000 145 125 180 10');await sleep(1200);const f=path.join(game,'qa-capture-request.txt');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,'r263-'+tag);await until(()=>!fs.existsSync(f));await sleep(800);assert(fs.existsSync(path.join(game,'screenshots/sanctuary-r263-'+tag+'.png')));}
async function volume(tag){const f=path.join(server,'qa-results/E25Rubble-'+tag+'.json');assert(!fs.existsSync(f));await command(`qa25 volume E25Rubble ${tag} 38968 88 -32 64 49 64`);await until(()=>fs.existsSync(f));return JSON.parse(fs.readFileSync(f));}
function check(name,ok,detail){rows.push({name,ok:!!ok,detail});console.log((ok?'PASS ':'FAIL ')+name);assert(ok,name);}
function readVar(b,o){let value=0,n=0,byte;do{byte=b[o++];value|=(byte&127)<<(7*n++);}while(byte&128&&n<5);return[value,o];}
(async()=>{
 await until(()=>log().includes('Done ('));c=mc.createClient({host:'127.0.0.1',port:25568,username:'E25Rubble',auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(c);
 c.on('login',()=>{c.ready=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:storm_rubble_v1\0tecnihardcore:cataclysm_v3\0tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3')});});c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));c.on('error',()=>{});
 c.on('world_event',p=>{if(active&&p.effectId===2001)breakEvents++;});
 c.on('custom_payload',p=>{if(!active||p.channel!=='tecnihardcore:storm_rubble_v1')return;let i=16;let age,n,count;[age,i]=readVar(p.data,i);[n,i]=readVar(p.data,i);[count,i]=readVar(p.data,i);assert(count<=32&&count>0);assert(age>=200);packets++;removed+=n;samples+=count;bytes+=p.data.length;});await until(()=>c.ready);
 await command('tecni desastre detener todos','gamemode spectator E25Rubble','execute in tecnihardcore:eventos run tp E25Rubble 39000 145 0','execute in tecnihardcore:eventos run forceload remove all','execute in tecnihardcore:eventos run forceload add 38944 -64 39056 64','execute in tecnihardcore:eventos run gamerule doMobSpawning false');await sleep(3500);
 await command('execute in tecnihardcore:eventos run fill 38968 87 -32 39031 87 31 bedrock');
 for(let x=38968;x<39032;x+=16)await command(`execute in tecnihardcore:eventos run fill ${x} 88 -32 ${x+15} 119 31 stone`,`execute in tecnihardcore:eventos run fill ${x} 120 -32 ${x+15} 135 31 oak_planks`);
 await command('execute in tecnihardcore:eventos run fill 38972 136 -28 38987 136 -13 obsidian','execute in tecnihardcore:eventos run fill 39012 136 12 39019 136 19 chest','execute in tecnihardcore:eventos run fill 38980 136 12 38987 136 19 oak_stairs[waterlogged=true]');
 const before=await volume('before');check('Isolated heavy structure fixture',before.solid>190000,before);
 await until(()=>log().includes('E25View joined the game'));await command('gamemode spectator E25View','execute in tecnihardcore:eventos run tp E25View 39000 145 125 180 10');
 active=true;await command('execute in tecnihardcore:eventos run tecni desastre iniciar tornado 39000 88 0 128 65 ancho 220 destruccion 20','qa25 benchmark E25Rubble rubble263 800');
 await sleep(19000);await capture('mass-uproot');await sleep(10000);await capture('dense-vortex');await sleep(16000);
 await command('tecni desastre detener todos');active=false;await sleep(3400);const after=await volume('after');
 check('Massive block removal within 35 active seconds',before.solid-after.solid>100000,{before:before.solid,after:after.solid});
 check('Actual removed blocks synchronized in bounded batches',packets>15&&samples>400&&removed>100000,{packets,samples,removed,bytes,breakEvents});
 const mark=log().length;await command('execute in tecnihardcore:eventos if block 38972 136 -28 obsidian run say QA263_OBSIDIAN_REMAINS','execute in tecnihardcore:eventos if block 39012 136 12 chest run say QA263_CHEST_REMAINS','execute in tecnihardcore:eventos unless block 38968 87 -32 bedrock run say QA263_BEDROCK_LOST','execute in tecnihardcore:eventos if entity @e[type=minecraft:falling_block] run say QA263_ENTITY_LEAK');
 const tail=log().slice(mark);check('Heavy blocks and chests uprooted; bedrock retained; no falling entities',!tail.includes('[Server] QA263_'),tail);
 const ticks=JSON.parse(fs.readFileSync(path.join(server,'qa-results/rubble263-ticks.json')));check('Tick p95 below 50ms during mass uprooting',ticks.p95Ms<50,ticks);
 await capture('stopped');rows.push({name:'Native captures of airborne blocks and cleanup',ok:true});
})().catch(e=>{console.error(e.stack);rows.push({name:'error',ok:false,detail:e.stack});process.exitCode=1;}).finally(async()=>{try{await command('tecni desastre detener todos');}catch{}if(c)c.end();fs.writeFileSync(path.join(out,'server-results.json'),JSON.stringify(rows,null,2));process.exit(process.exitCode||0);});
