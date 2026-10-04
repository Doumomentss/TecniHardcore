const fs=require('fs'),path=require('path'),assert=require('assert'),graphics=require('../launcher/graphics');
const base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),game=path.join(base,'game'),out=path.resolve('tools/test-runtime/weather261'),rows=[];
assert(fs.existsSync(path.join(server,'.tecni-test-world')));const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,ms=90000){let end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(100);}throw Error('timeout');}
async function command(...lines){let f=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,lines.join('\n')+'\n');await until(()=>!fs.existsSync(f));await sleep(200);}
async function request(name){let f=path.join(game,'qa-capture-request.txt');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,name);await until(()=>!fs.existsSync(f));await sleep(name==='graphics-reload'?12000:700);}
async function capture(name){await request('w261-'+name);assert(fs.existsSync(path.join(game,'screenshots/sanctuary-w261-'+name+'.png')));rows.push({name,ok:true});console.log('PASS screenshot '+name);}
async function stop(){await command('tecni desastre detener todos');await sleep(3300);}
(async()=>{
 await until(()=>fs.existsSync(path.join(out,'server-results.json')));assert(JSON.parse(fs.readFileSync(path.join(out,'server-results.json'))).every(r=>r.ok));
 await until(()=>fs.readFileSync(path.join(server,'logs/latest.log'),'utf8').includes('E25View joined the game'));await stop();
 await command('gamemode spectator E25View','execute in tecnihardcore:eventos run tp E25View 30000.5 145 125.5 180 20','execute in tecnihardcore:eventos run tecni desastre iniciar tornado 30000 96 0 256 50 ancho 120 destruccion 0');
 await sleep(31000);await capture('tornado-slope');await stop();
 await command('execute in tecnihardcore:eventos run tp E25View 30400.5 110 24.5 180 16','execute in tecnihardcore:eventos run fill 30380 95 -20 30420 95 20 stone','execute in tecnihardcore:eventos run tecni desastre iniciar meteoritos 30400 96 0 192 35 destruccion 0');
 await sleep(11300);await capture('meteor-flight');await sleep(1300);await capture('meteor-impact');await stop();
 await command('execute in tecnihardcore:eventos run tp E25View 30410.5 108 12.5 145 40','execute in tecnihardcore:eventos run tecni desastre iniciar terremoto 30400 96 0 192 35 destruccion 4');await sleep(16000);await capture('quake-fracture');await stop();
 await graphics.apply(game,'quality',path.resolve('installer_payload'));await request('graphics-reload');
 await command('execute in tecnihardcore:eventos run tp E25View 30400.5 110 24.5 180 16','execute in tecnihardcore:eventos run tecni desastre iniciar meteoritos 30400 96 0 192 35 destruccion 0');await sleep(11400);await capture('meteor-quality');await stop();
 const log=fs.readFileSync(path.join(game,'logs/latest.log'),'utf8');assert(!graphics.shaderError(log)&&!log.includes('Reported exception thrown!'));rows.push({name:'Native terrain/meteor geometry and quality shader stay active',ok:true});
 await graphics.apply(game,'optimized',path.resolve('installer_payload'));await request('graphics-reload');await capture('clear');
})().catch(e=>{console.error(e.message);rows.push({name:'error',ok:false,detail:e.stack});process.exitCode=1;}).finally(async()=>{try{await stop();}catch{}fs.writeFileSync(path.join(out,'native-results.json'),JSON.stringify(rows,null,2));});
