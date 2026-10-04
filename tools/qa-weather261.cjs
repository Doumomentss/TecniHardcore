const fs=require('fs'),path=require('path'),assert=require('assert'),mc=require('./test-runtime/node_modules/minecraft-protocol');
const server=path.resolve('tools/test-runtime/expansion25/server'),out=path.resolve('tools/test-runtime/weather261');
assert(fs.existsSync(path.join(server,'.tecni-test-world')));fs.mkdirSync(out,{recursive:true});
const output=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const rows=[],snapshots=[],sleep=ms=>new Promise(r=>setTimeout(r,ms)),log=()=>fs.readFileSync(path.join(server,'logs/latest.log'),'utf8');let c,blocks=0,record=false;
async function until(fn,ms=60000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(150);}throw Error('timeout');}
async function command(...lines){const f=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,lines.join('\n')+'\n');await until(()=>!fs.existsSync(f));await sleep(200);}
function check(name,ok,detail){rows.push({name,ok:!!ok,detail});output((ok?'PASS ':'FAIL ')+name);if(!ok)throw Error(name);}
function decode(b){let n=16;const type=b[n++];n+=8;function vi(){let v=0,shift=0,a;do{a=b[n++];v|=(a&127)<<shift;shift+=7;}while(a&128);return v;}
 const radius=vi(),age=vi(),duration=vi(),width=vi(),destruction=b[n++],state=b[n++];const x=b.readDoubleBE(n),y=b.readDoubleBE(n+8),z=b.readDoubleBE(n+16),crown=b.readDoubleBE(n+24);n+=32;const count=vi(),marks=[];for(let i=0;i<count;i++){marks.push({x:b.readDoubleBE(n),y:b.readDoubleBE(n+8),z:b.readDoubleBE(n+16)});n+=24;}return {type,radius,age,duration,width,destruction,state,x,y,z,crown,marks};}
async function stop(){await command('tecni desastre detener todos');await sleep(3300);}
async function fill(x=30400){for(let y=64;y<=92;y+=4)await command(`execute in tecnihardcore:eventos run fill ${x-32} ${y} -32 ${x+32} ${y+3} 32 stone`);await command(`execute in tecnihardcore:eventos run fill ${x-32} 95 -32 ${x+32} 95 32 dirt`);}
async function terrain(tag){await command('qa25 terrain E25Ground '+tag+' 30400 0');return JSON.parse(fs.readFileSync(path.join(server,'qa-results/E25Ground-'+tag+'.json')));}
async function report(tag){await command('qa25 report E25Ground '+tag);return JSON.parse(fs.readFileSync(path.join(server,'qa-results/E25Ground-'+tag+'.json')));}
(async()=>{
 await until(()=>log().includes('Done ('));c=mc.createClient({host:'127.0.0.1',port:25568,username:'E25Ground',auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(c);
 c.on('login',()=>{c.ready=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:cataclysm_v3')});});
 c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));c.on('block_change',()=>{if(record)blocks++;});c.on('multi_block_change',p=>{if(record)blocks+=p.records.length;});
 c.on('custom_payload',p=>{if(p.channel==='tecnihardcore:cataclysm_v3')snapshots.push(decode(p.data));});c.on('error',()=>{});await until(()=>c.ready);await sleep(1500);
 await command('tecni desastre detener todos','gamemode creative E25Ground','execute in tecnihardcore:eventos run tp E25Ground 30000.5 96 0.5','execute in tecnihardcore:eventos run forceload add 29952 -48 30048 48','execute in tecnihardcore:eventos run forceload add 30352 -48 30448 48');await sleep(1800);
 await command('execute in tecnihardcore:eventos run fill 29964 63 -36 30036 63 36 stone');
 for(let y=64;y<96;y+=8)await command(`execute in tecnihardcore:eventos run fill 30008 ${y} -36 30036 ${y+7} 36 stone`);
 await command('execute in tecnihardcore:eventos run fill 29965 120 -35 30035 120 35 oak_planks');
 let offset=log().length;snapshots.length=0;await command('execute in tecnihardcore:eventos run tecni desastre iniciar tornado 30000 96 0 128 55 ancho 120 destruccion 0');
 check('Roof no longer rejects tornado',log().slice(offset).includes('Desastre preparado'));
 await sleep(1000);check('Tip begins on raised terrain below roof',snapshots.some(p=>p.type===0&&p.y===96));
 await sleep(32500);const valid=snapshots.filter(p=>p.type===0&&p.state===0);
 check('Moving tip descends cliff to lower floor',valid.some(p=>p.y===64),{min:Math.min(...valid.map(p=>p.y)),max:Math.max(...valid.map(p=>p.y))});
 check('Funnel extends below its crown on descent',valid.some(p=>p.crown-p.y>220));
 check('Server position moves at expected faster angular speed',valid.some(p=>p.age>300&&Math.abs(p.x-(30000.5+24*Math.cos((p.age-200)/400)))<1e-6));await stop();
 offset=log().length;await command('execute in tecnihardcore:eventos run tecni desastre iniciar tornado 30000 300 0 128 30 ancho 120 destruccion 0');check('High requested Y accepted without 200-block world clearance',log().slice(offset).includes('Desastre preparado'));await stop();
 await command('execute in tecnihardcore:eventos run forceload add 30800 0','execute in tecnihardcore:eventos run setblock 30800 190 0 stone');offset=log().length;
 await command('execute in tecnihardcore:eventos run tecni desastre iniciar tornado 30800 300 0 32 30 ancho 120 destruccion 0');check('Only initial loaded chunk required',log().slice(offset).includes('Desastre preparado'));await stop();
 await command('execute in tecnihardcore:eventos run tp E25Ground 30400.5 96 0.5');await fill();snapshots.length=0;blocks=0;record=true;
 await command('execute in tecnihardcore:eventos run tecni desastre iniciar meteoritos 30400 96 0 128 30 destruccion 0');await sleep(13000);record=false;
 check('Meteors have visible impact marks for creative observers',snapshots.some(p=>p.type===4&&p.age>=200&&p.marks.length>0));const zeroTerrain=await terrain('meteor-zero');check('Meteor destruction zero never edits terrain',zeroTerrain.air===0,zeroTerrain);await stop();
 await command('gamemode survival E25Ground','tecni vidas E25Ground 3','clear E25Ground','effect clear E25Ground','effect give E25Ground instant_health 1 10 true','execute in tecnihardcore:eventos run tp E25Ground 30400.5 96 0.5');
 const before=await report('meteor-before');await command('execute in tecnihardcore:eventos run tecni desastre iniciar meteoritos 30400 96 0 128 30');await sleep(13200);const after=await report('meteor-after');
 check('Meteor applies real server damage to stationary survivor',after.health<before.health&&after.lives===before.lives,{before:before.health,after:after.health});await stop();await command('gamemode creative E25Ground');
 for(const level of [1,2,3,4]){await fill();blocks=0;record=true;await command(`execute in tecnihardcore:eventos run tecni desastre iniciar meteoritos 30400 96 0 128 30 destruccion ${level}`);await sleep(14500);record=false;const actual=await terrain('crater-'+level);check('Meteor destruction '+level+' makes a crater',actual.air>0,actual);await stop();}
 await fill();blocks=0;record=true;await command('execute in tecnihardcore:eventos run tecni desastre iniciar terremoto 30400 96 0 128 30 destruccion 4','qa25 benchmark E25Ground quake261 360');await sleep(17000);record=false;
 const actualQuake=await terrain('quake');check('Quake 4 removes substantial connected terrain',actualQuake.air>=350&&actualQuake.surface>=40,actualQuake);offset=log().length;
 await command('execute in tecnihardcore:eventos run execute if block 30403 80 -2 air run say QA_DEEP_FRACTURE','execute in tecnihardcore:eventos run execute if block 30403 79 -2 stone run say QA_FRACTURE_BOTTOM');
 check('Quake fracture is open from surface to sixteen blocks deep',log().slice(offset).includes('QA_DEEP_FRACTURE')&&log().slice(offset).includes('QA_FRACTURE_BOTTOM'));await sleep(2500);const ticks=JSON.parse(fs.readFileSync(path.join(server,'qa-results/quake261-ticks.json')));check('Large fracture retains tick p95 below 50ms',ticks.p95Ms<50,ticks);await stop();
 const rejection=log().length;await command('tecni desastre iniciar meteoritos 0 96 0 128 30 destruccion 4');check('Spawn protection retained for destructive meteor command',log().slice(rejection).includes('spawn'));
})().catch(e=>{rows.push({name:'error',ok:false,detail:e.stack});output(e.message);process.exitCode=1;}).finally(async()=>{try{await stop();}catch{}if(c)c.end();fs.writeFileSync(path.join(out,'server-results.json'),JSON.stringify(rows,null,2));process.exit(process.exitCode||0);});
