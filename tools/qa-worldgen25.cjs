// New terrain only, in the restored QA world. Existing production regions are never touched.
const fs=require('fs'),path=require('path'),assert=require('assert');const mc=require('./test-runtime/node_modules/minecraft-protocol');
const base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),out=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));async function until(fn,ms=180000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(250);}throw Error('Generation QA timeout');}
async function command(...lines){const p=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,lines.join('\n')+'\n');await until(()=>!fs.existsSync(p));await sleep(1500);}
let c;
(async()=>{
 assert(fs.existsSync(path.join(server,'.tecni-test-world')));c=mc.createClient({host:'127.0.0.1',port:25568,username:'E25Gen',auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(c);c.on('error',()=>{});c.on('login',()=>{c.logged=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3')});});c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));await until(()=>c.logged);await command('gamemode spectator E25Gen');
 const offset=fs.statSync(path.join(server,'logs/latest.log')).size,visits=[];
 for(const [dimension,biome] of [['minecraft:overworld','natures_spirit:maple_woodlands'],['minecraft:overworld','regions_unexplored:maple_forest'],['minecraft:the_nether','regions_unexplored:blackstone_basin']]){
  const start=fs.statSync(path.join(server,'logs/latest.log')).size;
  await command(`execute in ${dimension} positioned 100000 90 100000 run locate biome ${biome}`);let match;
  await until(()=>{match=fs.readFileSync(path.join(server,'logs/latest.log')).subarray(start).toString().match(/\[(-?\d+), (?:~|-?\d+), (-?\d+)\]/);return !!match;});
  await command(`execute in ${dimension} run tp E25Gen ${match[1]}.5 ${dimension==='minecraft:the_nether'?100:160} ${match[2]}.5`);await sleep(20000);visits.push({dimension,biome,x:+match[1],z:+match[2]});out('PASS: generated '+biome+' at '+match[1]+', '+match[2]);
 }
 await command('execute in minecraft:overworld positioned 100000 90 100000 run locate structure minecraft:village_plains','execute in minecraft:the_nether positioned 100000 90 100000 run locate structure betterfortresses:fortress','save-all flush');
 const text=fs.readFileSync(path.join(server,'logs/latest.log')).subarray(offset).toString();fs.writeFileSync(path.join(base,'worldgen-results.json'),JSON.stringify({visits,commands:text,healthy:!text.includes('Encountered an unexpected exception')},null,2));
})().catch(e=>{out(e.stack);process.exitCode=1;}).finally(async()=>{c?.end();await sleep(12000);process.exit(process.exitCode||0);});
