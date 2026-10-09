// Network proof with a fake account, only in the explicitly marked fixture.
const fs=require('fs'),path=require('path'),assert=require('assert/strict'),crypto=require('crypto');
const mc=require('./test-runtime/node_modules/minecraft-protocol');
const root=path.resolve('tools/test-runtime/social27/server');
assert(fs.existsSync(path.join(root,'.tecni-test-world')));
const manifest=require('../assets/npc-skins/manifest.json');
const configPath=path.join(root,'config/tecnihardcore/npcs.json');
const config=JSON.parse(fs.readFileSync(configPath));
for(const [name,hash] of Object.entries(manifest))config[name].skinHash=hash;
fs.writeFileSync(configPath,JSON.stringify(config,null,2));
fs.writeFileSync(path.join(root,'qa-commands.txt'),'tecni npc recargar\n');
const varint=n=>{const a=[];do{let v=n&127;n>>>=7;if(n)v|=128;a.push(v);}while(n);return Buffer.from(a);};
const str=s=>Buffer.concat([varint(Buffer.byteLength(s)),Buffer.from(s)]);
function readVar(b,o){let n=0;for(let k=0;k<35;k+=7){let v=b[o.i++];n|=(v&127)<<k;if(!(v&128))return n;}throw Error('varint');}
const sleep=n=>new Promise(r=>setTimeout(r,n));
const original=console.log;console.log=()=>{};
const client=mc.createClient({host:'127.0.0.1',port:25569,username:'E25CivicA',auth:'offline',version:'1.20.1'});
require('./test-pack-handshake.cjs')(client);
const received=new Map();let error,logged=false;
client.on('login',()=>{logged=true;client.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:npc_skin_data_v1\0fiw-mods-api:challenge')});});
client.on('packet',(p,m)=>{
 if(m.name==='position')client.write('teleport_confirm',{teleportId:p.teleportId});
 if(m.name!=='custom_payload')return;
 if(p.channel==='fiw-mods-api:challenge')client.write('custom_payload',{channel:'fiw-mods-api:response',data:Buffer.concat([varint(1),str('tecnihardcore'),str('2.7.0'),str('qa-only'),varint(0),varint(0),varint(0),varint(0),p.data])});
 if(p.channel==='tecnihardcore:npc_skin_data_v1'){
  const o={i:0},length=readVar(p.data,o),hash=p.data.subarray(o.i,o.i+length).toString();o.i+=length;
  const size=readVar(p.data,o),png=p.data.subarray(o.i,o.i+size);
  assert.equal(crypto.createHash('sha256').update(png).digest('hex'),hash);
  assert.equal(png.readUInt32BE(16),64);assert.equal(png.readUInt32BE(20),64);
  received.set(hash,png.length);
 }
});
client.on('error',e=>error=e);client.on('kick_disconnect',p=>error=Error(JSON.stringify(p)));
(async()=>{
 for(let i=0;!logged&&i<100;i++)await sleep(100);
 assert(logged,'login');await sleep(2500);
 for(const [name,hash] of Object.entries(manifest)){
  client.write('custom_payload',{channel:'tecnihardcore:npc_skin_request_v1',data:str(hash)});
  for(let i=0;!received.has(hash)&&i<70;i++)await sleep(100);
  if(error)throw error;assert(received.has(hash),'skin not delivered: '+name);
  await sleep(400);
 }
 const proof={passed:true,skins:Object.keys(manifest),hashes:manifest,verifiedServerPackets:3,clientUpdateRequired:false};
 fs.writeFileSync(path.join(root,'qa-results/npc-custom-skins-result.json'),JSON.stringify(proof,null,2));
 original('PASS: all 3 original NPC PNGs streamed and SHA-256 verified.');
})().catch(e=>{original(e.stack);process.exitCode=1;}).finally(()=>{client.end();setTimeout(()=>process.exit(process.exitCode||0),600);});
