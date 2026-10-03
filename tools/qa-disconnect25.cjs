// Repeated real protocol connections in the isolated fixture; catches unload crashes.
const fs=require('fs'),path=require('path'),assert=require('assert');
const log=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const mc=require('./test-runtime/node_modules/minecraft-protocol');
const server=path.resolve('tools/test-runtime/expansion25/server');
assert(fs.existsSync(path.join(server,'.tecni-test-world')));
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
 for(let i=0;i<12;i++){
  const name='E25Reconnect',c=mc.createClient({host:'127.0.0.1',port:25568,username:name,auth:'offline',version:'1.20.1'});
  require('./test-pack-handshake.cjs')(c);
  let logged=false,error;
  c.on('login',()=>{logged=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:cataclysm_v1\0tecnihardcore:event_v1')});});
  c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));
  c.on('error',e=>error=e);c.on('kick_disconnect',p=>error=Error(JSON.stringify(p)));
  for(let n=0;n<120&&!logged&&!error;n++)await sleep(200);
  assert(logged&&!error,'Reconnect failed: '+error);await sleep(500);assert(c.state==='play');c.end();await sleep(1200);
  assert(!fs.readFileSync(path.join(server,'logs/latest.log'),'utf8').includes('Encountered an unexpected exception'),'Server crashed after disconnect');
  log('PASS: connection/disconnection '+(i+1));
 }
 await sleep(15000);
 assert(!fs.readFileSync(path.join(server,'logs/latest.log'),'utf8').includes('Encountered an unexpected exception'));
 fs.writeFileSync(path.resolve('tools/test-runtime/expansion25/disconnect-results.json'),JSON.stringify({cycles:12,healthyAfterUnload:true,java:17,lithiumEntityByType:false},null,2));
 log('PASS: delayed unload remains healthy');process.exit(0);
})().catch(e=>{log(e.stack);process.exit(1);});
