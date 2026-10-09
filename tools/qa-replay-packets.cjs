// Local regression probe; only the marked test server on 25569 is allowed.
const fs=require('fs'),path=require('path'),mc=require('./test-runtime/node_modules/minecraft-protocol');
const root=path.resolve('tools/test-runtime/social27/server');
if(!fs.existsSync(path.join(root,'.tecni-test-world')))throw Error('Isolated marker required');
const wait=ms=>new Promise(r=>setTimeout(r,ms));
const v=n=>{let a=[];do{let x=n&127;n>>>=7;if(n)x|=128;a.push(x)}while(n);return Buffer.from(a)};
const s=x=>{let b=Buffer.from(x);return Buffer.concat([v(b.length),b])};
const name='E25ViewerQ',c=mc.createClient({host:'127.0.0.1',port:25569,username:name,auth:'offline',version:'1.20.1'});
require('./test-pack-handshake.cjs')(c);
let viewing=false,counts={},details=[];
c.on('packet',(p,m)=>{
 if(m.name==='position')c.write('teleport_confirm',{teleportId:p.teleportId});
 if(m.name==='custom_payload'&&p.channel==='fiw-mods-api:challenge')c.write('custom_payload',{channel:'fiw-mods-api:response',data:Buffer.concat([v(1),s('tecnihardcore'),s(require('../launcher/package.json').version),s('qa-viewer'),v(0),v(0),v(0),v(0),p.data])});
 if(viewing){counts[m.name]=(counts[m.name]||0)+1;if(['login','respawn','position','spawn_position','map_chunk','kick_disconnect','system_chat','game_state_change'].includes(m.name))details.push({type:m.name,...(['map_chunk'].includes(m.name)?{x:p.x,z:p.z}:p)});}
});
c.on('login',async()=>{
 if(viewing)return;
 c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from(['tecnihardcore:soul_v3','tecnihardcore:ritual_v3','tecnihardcore:altar_open_v3','fiw-mods-api:challenge'].join('\0'))});
 await wait(1800);fs.writeFileSync(path.join(root,'qa-commands.txt'),'qa25 authenticate '+name+'\nop '+name+'\n');await wait(1200);
 viewing=true;c.write('chat_command',{command:process.argv.slice(2).join(' ')||'tecni replay ver 10',timestamp:BigInt(Date.now()),salt:0n,argumentSignatures:[],messageCount:0,acknowledged:Buffer.alloc(3)});
 await wait(Number(process.env.TECNI_REPLAY_WAIT_MS||8000));fs.writeFileSync(path.join(root,'qa-results/replay-packets.json'),JSON.stringify({counts,details},(_,value)=>typeof value==='bigint'?value.toString():Buffer.isBuffer(value)?'<buffer>':value,2));console.log(JSON.stringify(counts));
 c.write('chat_command',{command:'replay view close',timestamp:BigInt(Date.now()),salt:0n,argumentSignatures:[],messageCount:0,acknowledged:Buffer.alloc(3)});await wait(500);c.end();setTimeout(()=>process.exit(),1000);
});c.on('error',e=>console.error(e));
