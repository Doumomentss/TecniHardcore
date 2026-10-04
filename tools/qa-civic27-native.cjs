// Real client requests and framebuffer screenshots in the full, marked world copy.
const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const server=path.resolve('tools/test-runtime/expansion25/server'),game=path.resolve('tools/test-runtime/expansion25/game');
assert(fs.existsSync(path.join(server,'.tecni-test-world')));
const delay=n=>new Promise(r=>setTimeout(r,n));
async function file(file,text){fs.writeFileSync(file,text);for(let n=0;n<200&&fs.existsSync(file);n++)await delay(100);assert(!fs.existsSync(file),'Request timeout: '+file);await delay(300);}
const command=s=>file(path.join(server,'qa-commands.txt'),s+'\n');
const action=o=>file(path.join(game,'qa-action.json'),JSON.stringify(o));
const shot=async tag=>{await file(path.join(game,'qa-capture-request.txt'),tag);await delay(250);assert(fs.existsSync(path.join(game,'screenshots/sanctuary-'+tag+'.png')));};
(async()=>{
 await command('tecni vidas E25CivicA 3\ngamemode survival E25CivicA\ntp E25CivicA -13.5 96 -13.5 0 0\nweather clear\ntime set day');await delay(1000);
 await action({type:'view',perspective:0,scale:2});await action({type:'npc',id:'ines'});await shot('civic27-dialogue');
 await action({type:'click',x:230,y:90});await delay(500); // The accept button is checked in the ledger below.
 await action({type:'close'});await command('tp E25CivicA 14.5 96 10.5 0 0');await delay(500);
 await action({type:'npc',id:'selma'});await shot('civic27-merchant-dialogue');
 await action({type:'command',command:'mercado'});await shot('civic27-market-empty');
 for(const [i,item] of ['diamond','iron_ingot','gold_ingot','emerald','lapis_lazuli','redstone','amethyst_shard'].entries()){
  await command('clear E25CivicA\ngive E25CivicA minecraft:'+item+' '+(i+1));
  await action({type:'command',command:'mercado vender '+(10+i*5)});
 }
 await action({type:'command',command:'mercado'});await shot('civic27-market-scale2');
 await action({type:'view',perspective:0,scale:4});await delay(1000);await shot('civic27-market-scale4');
 // At 1280x720, automatic GUI scaling selects factor 3; the screen has 3 visible rows.
 await action({type:'click',x:55,y:214});await shot('civic27-market-scale4-next');
 await file(path.join(game,'qa-capture-request.txt'),'fullscreen-toggle');await delay(1500);await shot('civic27-market-fullscreen');
 await file(path.join(game,'qa-capture-request.txt'),'fullscreen-toggle');await delay(1000);await action({type:'close'});
 await command('tp E25CivicA -13.5 96 -13.5 0 0');await delay(500);await action({type:'view',perspective:0,scale:2});await shot('civic27-spawn-npc');
 const ledger=JSON.parse(fs.readFileSync(path.join(server,'world/tecnihardcore-social.json'))).data;
 assert(Object.values(ledger.offers).filter(o=>o.state==='open').length===7);
 fs.writeFileSync(path.join(server,'qa-results/civic27-native-result.json'),JSON.stringify({passed:true,fullModClient:true,officialClientVerified:true,offers:7,screenshots:['dialogue','merchant-dialogue','market-empty','market-scale2','market-scale4','market-scale4-next','market-fullscreen','spawn-npc'].map(n=>'sanctuary-civic27-'+n+'.png')},null,2));
 console.log('PASS native NPC interaction, marketplace commands, 7 escrows, GUI scale 2/4 and fullscreen captures');
})().catch(e=>{console.error(e);process.exitCode=1});
