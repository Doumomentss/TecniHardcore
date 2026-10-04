const fs=require('fs'),path=require('path'),assert=require('assert'),mc=require('./test-runtime/node_modules/minecraft-protocol');
const server=path.resolve('tools/test-runtime/expansion25/server'),out=path.resolve('tools/test-runtime/tornado262');assert(fs.existsSync(path.join(server,'.tecni-test-world')));fs.mkdirSync(out,{recursive:true});
const print=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};const rows=[],sleep=ms=>new Promise(r=>setTimeout(r,ms));let c,packets=0;
const log=()=>fs.readFileSync(path.join(server,'logs/latest.log'),'utf8');
async function until(fn,ms=240000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(150);}throw Error('timeout');}
async function command(...lines){const file=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(file));fs.writeFileSync(file,lines.join('\n')+'\n');await until(()=>!fs.existsSync(file));await sleep(250);}
async function volume(tag){const file=path.join(server,'qa-results/E25Wind-'+tag+'.json');assert(!fs.existsSync(file));await command(`qa25 volume E25Wind ${tag} 34929 16 -71 143 260 143`);await until(()=>fs.existsSync(file));return JSON.parse(fs.readFileSync(file));}
async function stop(){await command('tecni desastre detener todos');await sleep(3500);}
function check(name,ok,detail){rows.push({name,ok:!!ok,detail});print((ok?'PASS ':'FAIL ')+name);if(!ok)throw Error(name);}
(async()=>{
 await until(()=>log().includes('Done ('));c=mc.createClient({host:'127.0.0.1',port:25568,username:'E25Wind',auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(c);
 c.on('login',()=>{c.ready=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:cataclysm_v3\0tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3')});});c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));c.on('custom_payload',p=>{if(p.channel==='tecnihardcore:cataclysm_v3')packets++;});c.on('error',()=>{});await until(()=>c.ready);
 await stop();await command('gamemode spectator E25Wind','execute in tecnihardcore:eventos run tp E25Wind 35000 100 0','execute in tecnihardcore:eventos run forceload remove all','execute in tecnihardcore:eventos run forceload add 34912 -96 35088 96','execute in tecnihardcore:eventos run gamerule randomTickSpeed 0','execute in tecnihardcore:eventos run gamerule doMobSpawning false');await sleep(4000);
 if(!process.argv.includes('--resume-placed')){const begin=log().length;await command('execute in tecnihardcore:eventos run place template dungeons_arise:keep_kayra/keep_kayra_main_0 34929 16 -71');await until(()=>log().slice(begin).includes('Loaded template'));await sleep(10000);}else assert(fs.existsSync(path.join(out,'kayra-placed.json')));
 const before=await volume('kayra-before');check('Real Keep Kayra template placed in isolated event dimension',before.solid>800000,before);
 await command('execute in tecnihardcore:eventos run tecni desastre iniciar tornado 35000 32 0 192 30 ancho 120 destruccion 4');await sleep(31000);await stop();const four=await volume('kayra-four');check('Level four remains a much lighter storm',four.solid<before.solid&&four.solid>before.solid*.9,{removed:before.solid-four.solid});
 let last=four,totalSeconds=0;
 for(let round=1;round<=3&&last.solid>before.solid*.2;round++){
  const offset=log().length;await command('execute in tecnihardcore:eventos run tecni desastre iniciar tornado 35000 32 0 512 190 ancho 600 destruccion 20',`qa25 benchmark E25Wind tornado262-${round} 2400`);
  check('Level twenty command accepted, run '+round,log().slice(offset).includes('Desastre preparado'));print('Running level twenty on Keep Kayra, 180 active seconds...');
  for(let t=0;t<6;t++){await sleep(30000);print('Keep Kayra destruction test: '+((round-1)*180+(t+1)*30)+' active seconds');}
  await sleep(10000);totalSeconds+=180;await stop();last=await volume('kayra-twenty-'+round);const fraction=1-last.solid/before.solid;print('Keep Kayra removed: '+(fraction*100).toFixed(1)+'%');rows.push({name:'Keep Kayra demolition checkpoint '+round,ok:true,detail:{before:before.solid,remaining:last.solid,removedFraction:fraction,activeSeconds:totalSeconds}});
  const ticks=JSON.parse(fs.readFileSync(path.join(server,`qa-results/tornado262-${round}-ticks.json`)));check('High destruction tick p95 below 50 ms, run '+round,ticks.p95Ms<50,ticks);
 }
 check('Level twenty demolishes at least eighty percent of real Keep Kayra',last.solid<=before.solid*.2,{original:before.solid,remaining:last.solid,activeSeconds:totalSeconds});check('Revised clients receive high-level storm packets',packets>0,{packets});
 const offset=log().length;await command('tecni desastre iniciar tornado 0 96 0 512 30 ancho 600 destruccion 20');check('Protected spawn still rejects extreme tornado',log().slice(offset).includes('spawn'));
})().catch(error=>{rows.push({name:'error',ok:false,detail:error.stack});print(error.message);process.exitCode=1;}).finally(async()=>{try{await stop();}catch{}if(c)c.end();fs.writeFileSync(path.join(out,'server-results.json'),JSON.stringify(rows,null,2));process.exit(process.exitCode||0);});
