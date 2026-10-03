// Only the restored world copy on localhost. No real player identities are used.
const mc=require('./test-runtime/node_modules/minecraft-protocol'),fs=require('fs'),path=require('path');
const print=console.log;console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const clients={},events=[],base=path.resolve('tools/test-runtime/content24');
for(const username of ['PruebaBaliza','Alma24']){
 const c=mc.createClient({host:'127.0.0.1',port:25566,username,auth:'offline',version:'1.20.1'});clients[username]=c;require('./test-pack-handshake.cjs')(c);
 c.on('login',()=>{c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:boss_fx_v1\0tecnihardcore:rescue_v1')});print(username+' joined isolated world');});
 c.on('packet',(p,m)=>{
  if(m.name==='position')c.write('teleport_confirm',{teleportId:p.teleportId});
  if(m.name==='custom_payload'&&['tecnihardcore:rescue_v1','tecnihardcore:ritual_v3','tecnihardcore:altar_open_v3'].includes(p.channel))events.push({at:Date.now(),username,channel:p.channel,hex:Buffer.from(p.data).toString('hex')});
  if(m.name==='custom_payload'&&p.channel==='tecnihardcore:soul_v3')events.push({at:Date.now(),username,lives:p.data.readInt32BE(0),resurrections:p.data[4]});
  if(m.name==='update_health')events.push({at:Date.now(),username,health:p.health});
 });
 c.on('error',e=>print(username+': '+e.message));c.on('kick_disconnect',p=>print(username+' disconnected: '+JSON.stringify(p)));
}
process.stdin.on('data',d=>{
 const cmd=d.toString().trim();if(cmd==='beacon')clients.PruebaBaliza.write('use_item',{hand:0,sequence:0});
 if(cmd==='respawn')for(const c of Object.values(clients))c.write('client_command',{actionId:0});
 if(cmd==='stop'){for(const c of Object.values(clients))c.end();fs.writeFileSync(path.join(base,'events.json'),JSON.stringify(events,null,2));setTimeout(()=>process.exit(),200);}
});
