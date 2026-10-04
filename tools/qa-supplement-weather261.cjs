const fs=require('fs'),path=require('path'),assert=require('assert');
const base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),game=path.join(base,'game'),out=path.resolve('tools/test-runtime/weather261'),rows=[];
assert(fs.existsSync(path.join(server,'.tecni-test-world')));
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,ms=90000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(100);}throw Error('timeout');}
async function command(...lines){const f=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,lines.join('\n')+'\n');await until(()=>!fs.existsSync(f));await sleep(200);}
async function request(name){const f=path.join(game,'qa-capture-request.txt');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,name);await until(()=>!fs.existsSync(f));await sleep(700);}
async function action(data){const f=path.join(game,'qa-action.json');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,JSON.stringify(data));await until(()=>!fs.existsSync(f));}
async function stop(){await command('tecni desastre detener todos');await sleep(3300);}
(async()=>{
 await stop();
 for(let y=80;y<=92;y+=4)await command(`execute in tecnihardcore:eventos run fill 30368 ${y} -32 30432 ${y+3} 32 stone`);
 await command('execute in tecnihardcore:eventos run tp E25View 30400.5 110 24.5 180 16','execute in tecnihardcore:eventos run tecni desastre iniciar meteoritos 30400 96 0 192 30 destruccion 0');
 await action({type:'view',particles:2,perspective:0});await request('fullscreen-toggle');await sleep(10000);await request('w261-meteor-minimal-fullscreen');
 await command('qa25 terrain E25View meteor-zero-proof 30400 0');
 const terrain=JSON.parse(fs.readFileSync(path.join(server,'qa-results/E25View-meteor-zero-proof.json')));
 assert.equal(terrain.air,0);assert.equal(terrain.surface,0);rows.push({name:'Destruction zero preserves actual server blocks, independently of packet notifications',ok:true,detail:terrain});console.log('PASS default zero preserves actual terrain');
 await stop();await request('fullscreen-toggle');await action({type:'view',particles:1,perspective:0});
 await command('execute in tecnihardcore:eventos run tp E25View 30000.5 145 125.5 180 20','execute in tecnihardcore:eventos run tecni desastre iniciar tornado 30000 96 0 256 50 ancho 120 destruccion 0');
 await sleep(31000);await request('w261-tornado-slope');rows.push({name:'Tornado terrain capture within render distance',ok:true});console.log('PASS tornado terrain capture');
})().catch(error=>{rows.push({name:'error',ok:false,detail:error.stack});console.error(error);process.exitCode=1;}).finally(async()=>{try{await stop();}catch{}fs.writeFileSync(path.join(out,'supplement-results.json'),JSON.stringify(rows,null,2));});
