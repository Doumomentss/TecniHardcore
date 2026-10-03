// Native framebuffer and timing measurements with two protocol actors in the copied world.
const fs=require('fs'),path=require('path'),assert=require('assert');
const mc=require('./test-runtime/node_modules/minecraft-protocol');
const base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),game=path.join(base,'game');
const graphics=require('../launcher/graphics'),results=[],clients=[];const output=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(test,timeout=90000){const end=Date.now()+timeout;while(Date.now()<end){if(test())return;await sleep(200);}throw Error('QA condition timeout');}
async function command(...lines){const p=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,lines.join('\n')+'\n');await until(()=>!fs.existsSync(p));await sleep(300);}
async function action(data){const p=path.join(game,'qa-action.json');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,JSON.stringify(data));await until(()=>!fs.existsSync(p));await sleep(300);}
async function request(name){const p=path.join(game,'qa-capture-request.txt');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,name);await until(()=>!fs.existsSync(p));await sleep(name==='graphics-reload'?12000:700);}
async function capture(name){const p=path.join(game,'screenshots','sanctuary-'+name+'.png'),old=fs.existsSync(p)?fs.statSync(p).mtimeMs:0;await request(name);await until(()=>fs.existsSync(p)&&fs.statSync(p).mtimeMs>old);}
function check(name,ok,detail){results.push({name,ok:!!ok,detail});output((ok?'PASS':'FAIL')+': '+name);}
async function client(name){const c=mc.createClient({host:'127.0.0.1',port:25568,username:name,auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(c);clients.push(c);c.on('error',e=>output(name+': '+e.message));c.on('login',()=>{c.logged=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:cataclysm_v1\0tecnihardcore:event_v1')});});c.on('position',p=>{c.pos=p;c.write('teleport_confirm',{teleportId:p.teleportId});});await until(()=>c.logged);return c;}
async function report(name,tag){await command(`qa25 report ${name} ${tag}`);return JSON.parse(fs.readFileSync(path.join(server,'qa-results',`${name}-${tag}.json`)));}
async function bench(name){const frameFile=path.join(game,'qa-'+name+'-frames.json'),tickFile=path.join(server,'qa-results',name+'-ticks.json');for(const file of [frameFile,tickFile])if(fs.existsSync(file))fs.renameSync(file,file+'.previous-'+Date.now());assert(clients.every(c=>c.state==='play'),'Both actors must remain connected during timing');await command(`qa25 benchmark E25View ${name} 440`);await action({type:'benchmark',name,seconds:20});let frames,ticks;await until(()=>{try{frames=JSON.parse(fs.readFileSync(frameFile));ticks=JSON.parse(fs.readFileSync(tickFile));return true;}catch{return false;}});assert(clients.every(c=>c.state==='play'),'Actor disconnected during timing');results.push({name,frames,ticks});output(name+': '+frames.meanFps.toFixed(1)+' FPS / tick p95 '+ticks.p95Ms.toFixed(2)+' ms');return {frames,ticks};}
(async()=>{
 await command('tecni desastre detener todos');await sleep(4000);const a=await client('E25Alpha'),b=await client('E25Beta');
 await command('tecni vidas E25Alpha 3','tecni vidas E25Beta 3','gamemode survival E25Alpha','gamemode survival E25Beta','gamemode spectator E25View','execute in tecnihardcore:eventos run tp E25Alpha 10000.5 95 0.5','execute in tecnihardcore:eventos run tp E25Beta 10012.5 95 0.5','execute in tecnihardcore:eventos run tp E25View 10000.5 104 32.5 180 12','effect give E25Alpha minecraft:resistance 600 255 true','effect give E25Beta minecraft:resistance 600 255 true','execute in tecnihardcore:eventos run fill 10010 101 -2 10015 101 3 glass');
 for(const c of [a,b])c.groundTimer=setInterval(()=>{if(c.state==='play'&&c.pos)c.write('position',{x:c.pos.x,y:c.pos.y,z:c.pos.z,onGround:true});},500);
 await action({type:'view',perspective:0,particles:1});await graphics.apply(game,'optimized',path.resolve('installer_payload'));await request('graphics-reload');
 const baseline=await bench('baseline-three-players');await capture('baseline-three-players');
 for(const type of (process.argv.includes('--benchmark')?['tornado']:['acida','terremoto','electrica','meteoritos','tornado'])){
   await command(`execute in tecnihardcore:eventos run tecni desastre iniciar ${type} 10000 95 0 96 90`);await sleep(11000);
   if(type==='tornado')await command('execute in tecnihardcore:eventos run tp E25View 10000.5 195 245.5 180 0');
   await capture(type+'-optimized');await action({type:'view',perspective:0,particles:2});await capture(type+'-minimal');await action({type:'view',perspective:0,particles:1});
   if(type==='tornado'){await command('execute in tecnihardcore:eventos run tp E25View 10000.5 195 245.5 180 0');const measure=await bench('tornado-three-players');check('Tornado tick p95 below 50 ms',measure.ticks.p95Ms<50,measure.ticks);}
   await command('tecni desastre detener todos');await sleep(4000);
   await command('execute in tecnihardcore:eventos run tp E25View 10000.5 104 32.5 180 12');
 }
 // Match the baseline camera to the huge funnel scene, after identical warm-up and options.
 await command('execute in tecnihardcore:eventos run tp E25View 10000.5 195 245.5 180 0');const distant=await bench('baseline-tornado-camera');const tornado=results.find(r=>r.name==='tornado-three-players');check('Optimized FPS loss under 25 percent',tornado.frames.meanFps>=distant.frames.meanFps*.75,{baseline:distant.frames.meanFps,tornado:tornado.frames.meanFps});
 await command('spark tps');
 for(const mode of (process.argv.includes('--benchmark')?[]:['quality','ultra-quality'])){
   await graphics.apply(game,mode,path.resolve('installer_payload'));await request('graphics-reload');
   const log=fs.readFileSync(path.join(game,'logs/latest.log'),'utf8');check(mode+' shader creates pipeline',/Using shaderpack: ComplementaryReimagined_r5.9.3.zip/.test(log)&&!graphics.shaderError(log));
   for(const type of ['tornado','acida','terremoto','electrica','meteoritos']){
     await command(`execute in tecnihardcore:eventos run tp E25View ${type==='tornado'?'10000.5 195 245.5 180 0':'10000.5 104 32.5 180 12'}`,`execute in tecnihardcore:eventos run tecni desastre iniciar ${type} 10000 95 0 96 60`);await sleep(12000);await capture(type+'-'+mode);await command('tecni desastre detener todos');await sleep(4000);
   }
 }
 if(!process.argv.includes('--benchmark')){await request('fullscreen-toggle');await command('execute in tecnihardcore:eventos run tecni desastre iniciar tornado 10000 95 0 96 60','execute in tecnihardcore:eventos run tp E25View 10000.5 195 245.5 180 0');await sleep(12000);await capture('tornado-fullscreen');await request('fullscreen-toggle');await command('tecni desastre detener todos');await sleep(4000);await capture('disasters-cleared');}
 await graphics.apply(game,'optimized',path.resolve('installer_payload'));await request('graphics-reload');check(process.argv.includes('--benchmark')?'Two actors remain connected after tornado benchmark':'Two actors remain connected after all five disasters',clients.every(c=>c.state==='play'));fs.writeFileSync(path.join(base,'weather-results.json'),JSON.stringify(results,null,2));
})().catch(e=>{output(e.stack);results.push({name:'exception',ok:false,detail:e.stack});process.exitCode=1;}).finally(async()=>{clients.forEach(c=>{clearInterval(c.groundTimer);c.end();});await sleep(12000);const log=fs.readFileSync(path.join(server,'logs/latest.log'),'utf8');check('Server remains healthy after observer test actors disconnect',!log.includes('Encountered an unexpected exception')&&!log.includes('Exception stopping the server'));fs.writeFileSync(path.join(base,'weather-results.json'),JSON.stringify(results,null,2));process.exit(results.some(r=>r.ok===false)?1:process.exitCode||0);});
