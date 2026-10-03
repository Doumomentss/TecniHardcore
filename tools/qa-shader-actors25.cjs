const fs=require('fs'),path=require('path');console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const mc=require('./test-runtime/node_modules/minecraft-protocol'),clients=[];
const base=path.resolve('tools/test-runtime/expansion25');
function connect(name){const c=mc.createClient({host:'127.0.0.1',port:25568,username:name,auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(c);clients.push(c);c.on('error',()=>{});c.on('login',()=>{c.logged=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:cataclysm_v1\0tecnihardcore:event_v1')});});c.on('position',p=>{c.pos=p;c.write('teleport_confirm',{teleportId:p.teleportId});});return c;}
connect('E25ShaderA');connect('E25ShaderB');
const timer=setInterval(()=>{if(clients.every(c=>c.logged)&&!fs.existsSync(path.join(base,'shader-actors-ready.json')))fs.writeFileSync(path.join(base,'shader-actors-ready.json'),'{"ready":true}');for(const c of clients)if(c.state==='play'&&c.pos)c.write('position',{x:c.pos.x,y:c.pos.y,z:c.pos.z,onGround:true});},500);
setTimeout(()=>{clearInterval(timer);clients.forEach(c=>c.end());setTimeout(()=>process.exit(0),500);},600000);
