// Force termination is restricted to the marked localhost QA host; never port 25565.
const fs=require('fs'),path=require('path'),assert=require('assert'),cp=require('child_process');
const mc=require('./test-runtime/node_modules/minecraft-protocol');
const base=path.resolve('tools/test-runtime/expansion25'),server=path.join(base,'server'),clients=[],results=[];
const out=console.log.bind(console);console.log=()=>{};console.warn=()=>{};console.error=()=>{};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,ms=120000){const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await sleep(250);}throw Error('Recovery test timeout');}
async function command(...lines){const p=path.join(server,'qa-commands.txt');await until(()=>!fs.existsSync(p));fs.writeFileSync(p,lines.join('\n')+'\n');await until(()=>!fs.existsSync(p));await sleep(500);}
async function connect(name){const c=mc.createClient({host:'127.0.0.1',port:25568,username:name,auth:'offline',version:'1.20.1'});clients.push(c);require('./test-pack-handshake.cjs')(c);c.on('error',()=>{});c.on('login',()=>{c.logged=true;c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:event_v1\0tecnihardcore:mount_input_v1')});});c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));await until(()=>c.logged);return c;}
async function report(name,tag){await command(`qa25 report ${name} ${tag}`);return JSON.parse(fs.readFileSync(path.join(server,'qa-results',`${name}-${tag}.json`)));}
function state(){return JSON.parse(fs.readFileSync(path.join(server,'world/tecnihardcore-expansion.json')));}
function check(name,ok,detail){results.push({name,ok:!!ok,detail});out((ok?'PASS':'FAIL')+': '+name);assert(ok,name);}
(async()=>{
 assert(fs.existsSync(path.join(server,'.tecni-test-world')));
 for(const name of ['E25Alpha','E25Beta','E25Crash'])await connect(name);
 await command('op E25Alpha','tecni desastre detener todos',...['E25Alpha','E25Beta','E25Crash'].flatMap((n,i)=>[`tecni vidas ${n} 3`,`gamemode survival ${n}`,`clear ${n}`,`tp ${n} ${10000.5+i*4} 95 10000.5`]),'give E25Alpha emerald 11','experience set E25Alpha 13 levels','tecni montura dar E25Crash 3');
 const before=await report('E25Alpha','restart-before');
 const offset=fs.statSync(path.join(server,'logs/latest.log')).size;
 await command('execute as E25Alpha run tecni evento preparar circuito ensayo');let id;
 await until(()=>{id=fs.readFileSync(path.join(server,'logs/latest.log')).subarray(offset).toString().match(/Event arena ready: ([a-f0-9-]{36})/)?.[1];return id;});
 await command(`execute as E25Alpha run tecni evento entrar ${id}`,`tecni evento iniciar ${id}`,'qa25 mount E25Crash 3','qa25 mount-damage E25Crash 10');
 const riding=await report('E25Crash','restart-mount'),stamp=String(Date.now());
 await command(`qa25 reward-partial E25Beta ${stamp}`,'tecni desastre iniciar acida 10000 95 10000 32 180','save-all flush');
 const record={...state().mounts[riding.mount]},reward=Object.keys(state().rewards).find(k=>k.startsWith('qa-partial:'+stamp+':'));
 check('Durable interrupted delivery contains exactly its first part',state().rewards[reward].state==='delivering');
 // Verify exact process identity immediately before terminating our QA JVM.
 cp.execFileSync('powershell.exe',['-NoProfile','-Command',"$qaListen=Get-NetTCPConnection -LocalPort 25568 -State Listen; $qaProcess=Get-CimInstance Win32_Process -Filter ('ProcessId='+$qaListen.OwningProcess); if($qaProcess.CommandLine -notlike '*-Dtecni.testServer=true*' -or $qaProcess.CommandLine -notlike '*fabric-server-launch.jar*'){throw 'Refusing non-QA process'}; Stop-Process -Id $qaProcess.ProcessId -Force"],{stdio:'pipe'});
 await sleep(3000);clients.forEach(c=>c.end());clients.length=0;
 const stream=fs.openSync(path.join(base,'restart-server.log'),'a');
 const runtime=fs.readdirSync(path.join(base,'game/runtime')).find(n=>n.startsWith('jdk-17.'));
 assert(runtime,'Verified Java 17 runtime required');
 const host=cp.spawn(path.join(base,'game/runtime',runtime,'bin/java.exe'),['-Xms512M','-Xmx4G','-Dtecni.testServer=true','-Dtecni.testRewards=true','-jar','fabric-server-launch.jar','nogui'],{cwd:server,windowsHide:true,detached:true,stdio:['ignore',stream,stream]});host.on('error',e=>out(e.stack));host.unref();fs.closeSync(stream);
 await until(()=>{try{cp.execFileSync('powershell.exe',['-NoProfile','-Command',"if(-not(Get-NetTCPConnection -LocalPort 25568 -State Listen -ErrorAction SilentlyContinue)){exit 1}"],{stdio:'ignore'});return /Done \(.*\)!/.test(fs.readFileSync(path.join(server,'logs/latest.log'),'utf8'));}catch{return false;}},240000);
 for(const name of ['E25Alpha','E25Beta','E25Crash'])await connect(name);await sleep(3000);
 const after=await report('E25Alpha','restart-after');
 check('Crash during practice restores exact inventory, lives and original dimension',before.inventory===after.inventory&&before.lives===after.lives&&before.dimension===after.dimension,{before,after});
 check('Temporary mount cannot survive cancelled event restart',!Object.values(state().mounts).some(m=>m.temporary&&m.state==='activa'));
 check('Recovery snapshot removed only after restoration',!fs.readdirSync(path.join(server,'world/tecni-event-recovery')).some(n=>n.endsWith('.nbt')));
 const mail=await report('E25Beta','restart-mail');
 check('Partial delivery recovers each material once',state().rewards[reward].state==='claimed'&&/Count:4b[^}]*id:"minecraft:diamond"/.test(mail.inventory)&&/Count:1b[^}]*id:"minecraft:gold_block"/.test(mail.inventory)&&/Count:2b[^}]*id:"minecraft:iron_block"/.test(mail.inventory),mail.inventory);
 await command('execute as E25Beta run tecni recompensas');const repeated=await report('E25Beta','restart-mail-repeat');check('Recovered delivery repeated claim is idempotent',mail.inventory.split(reward).length===4&&repeated.inventory.split(reward).length===4);
 const now=state().mounts[riding.mount];check('Personal mount preserves canonical damaged health and boost across restart',now.health===record.health&&now.boostAt===record.boostAt&&now.state!=='muerta',{record,now});
 await command('tecni desastre listar','tecni evento listar');
 const tail=fs.readFileSync(path.join(server,'logs/latest.log'),'utf8').slice(-6000);check('Interrupted disasters and event never auto resume',tail.includes('No hay eventos preparados.')&&!/ADMIN .*started event/.test(tail));
})().catch(e=>{out(e.stack);results.push({name:'exception',ok:false,detail:e.stack});process.exitCode=1;}).finally(async()=>{clients.forEach(c=>c.end());await sleep(15000);fs.writeFileSync(path.join(base,'restart-results.json'),JSON.stringify(results,null,2));process.exit(process.exitCode||0);});
