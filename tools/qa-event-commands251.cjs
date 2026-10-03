// Exercise real command trees on the explicitly isolated server, never production.
const fs=require('fs'),path=require('path'),assert=require('assert');
const mc=require('./test-runtime/node_modules/minecraft-protocol');
const root=path.resolve('tools/test-runtime/event251/server'),results=[],clients=[];
assert(fs.existsSync(path.join(root,'.tecni-test-world')));
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const log=()=>fs.readFileSync(path.join(root,'logs/latest.log'),'utf8');
const record=(name,ok)=>{results.push({name,ok:!!ok});console.log((ok?'PASS':'FAIL')+': '+name);assert(ok,name);};
async function until(fn){for(let i=0;i<150;i++){if(fn())return;await sleep(400);}throw Error('Timed out');}
async function command(lines){const f=path.join(root,'qa-commands.txt');await until(()=>!fs.existsSync(f));fs.writeFileSync(f,[lines].flat().join('\n')+'\n');await until(()=>!fs.existsSync(f));await sleep(250);}
function player(name){const c=mc.createClient({host:'127.0.0.1',port:25568,username:name,auth:'offline',version:'1.20.1'});require('./test-pack-handshake.cjs')(c);clients.push(c);c.on('login',()=>{c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('tecnihardcore:soul_v3\0tecnihardcore:ritual_v3\0tecnihardcore:altar_open_v3\0tecnihardcore:event_v1')});c.ready=true;});c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));c.on('error',e=>console.log('Client: '+e.message));return c;}
async function prepare(type){const offset=log().length;await command('tecni evento preparar '+type+' ensayo');await until(()=>/Event arena ready:/.test(log().slice(offset)));const text=log().slice(offset);return {uuid:text.match(/Event arena ready: ([a-f0-9-]{36})/)[1],number:text.match(/Evento (\d+)/)[1],text};}
const report=tag=>JSON.parse(fs.readFileSync(path.join(root,'qa-results/E25Short-'+tag+'.json')));
(async()=>{
 await until(()=>/Done \(.*\)!/.test(log()));
 const c=player('E25Short');await until(()=>c.ready);await sleep(1500);
 await command(['tecni evento detener','op E25Short','tecni vidas E25Short 3','gamemode survival E25Short','effect clear E25Short','forceload add 9990 9990 10010 10010','tp E25Short 10000.5 95 10000.5']);await sleep(1500);
 await command(['fill 9997 94 9997 10004 94 10004 stone','clear E25Short','give E25Short diamond 7','qa25 report E25Short before']);
 let offset=log().length;await command(['tecni evento iniciar','tecni evento detener','execute as E25Short run tecni evento entrar']);record('Missing event produces useful feedback',log().slice(offset).includes('No hay un evento preparado'));
 const first=await prepare('defensa');record('Public instructions use no UUID',first.text.includes('/tecni evento entrar')&&!/Entrar con \/tecni evento entrar [a-f0-9-]{36}/.test(first.text));
 offset=log().length;await command('tecni evento iniciar');record('Cannot start without participants',log().slice(offset).includes('no hay participantes'));
 await command(['execute as E25Short run tecni evento entrar','qa25 report E25Short entered']);record('Join without ID reaches event dimension',report('entered').dimension==='tecnihardcore:eventos');
 offset=log().length;await command('tecni evento iniciar');record('Start without ID begins the enrolled event',log().slice(offset).includes('started event'));
 offset=log().length;await command('tecni evento iniciar');record('Repeated start cannot restart event',log().slice(offset).includes('El evento ya comenz'));
 await command(['tecni evento detener','qa25 report E25Short after']);record('Stop without ID restores original inventory and lives',report('before').inventory===report('after').inventory&&report('before').lives===report('after').lives&&report('after').dimension===report('before').dimension);
 const second=await prepare('circuito');offset=log().length;await command('tecni evento detener '+first.number);record('Old short alias cannot stop replacement event',log().slice(offset).includes('disponible. Consulta /tecni evento listar'));
 await command(['execute as E25Short run tecni evento entrar '+second.number,'qa25 report E25Short numbered']);record('Short numeric reference also joins current event',report('numbered').dimension==='tecnihardcore:eventos');
 await command(['tecni evento detener '+second.uuid,'qa25 report E25Short legacy']);record('Legacy UUID command remains compatible',report('legacy').dimension===report('before').dimension);
 const third=await prepare('tormenta');await command(['execute as E25Short run tecni evento entrar','execute as E25Short run tecni evento salir','qa25 report E25Short left']);record('No-ID entry works for third preset and voluntary exit restores state',report('left').inventory===report('before').inventory&&report('left').lives===3);
 await command('tecni evento detener '+third.number);record('Short numeric stop works',log().includes('Evento detenido.'));
 clients.forEach(c=>c.end());await sleep(2000);record('No server exception during command tests',!log().includes('Encountered an unexpected exception'));
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{clients.forEach(c=>c.end());fs.writeFileSync(path.resolve('tools/test-runtime/event251/event-command-results.json'),JSON.stringify(results,null,2));setTimeout(()=>process.exit(process.exitCode||0),250);});
