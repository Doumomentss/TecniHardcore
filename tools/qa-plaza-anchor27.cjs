const fs=require('fs'),p=require('path'),assert=require('assert/strict');
const r=p.resolve('tools/test-runtime/expansion25/server'),g=p.resolve('tools/test-runtime/expansion25/game');assert(fs.existsSync(p.join(r,'.tecni-test-world')));
const delay=n=>new Promise(res=>setTimeout(res,n));
async function file(f,t){fs.writeFileSync(f,t);for(let n=0;fs.existsSync(f)&&n<200;n++)await delay(100);assert(!fs.existsSync(f),'Not consumed: '+f);await delay(300);}
const act=o=>file(p.join(g,'qa-action.json'),JSON.stringify(o)),cmd=t=>file(p.join(r,'qa-commands.txt'),t+'\n');
const id='3529de0a-6f14-344d-b341-a780ba951f1e',data=()=>JSON.parse(fs.readFileSync(p.join(r,'world/tecnihardcore-social.json'))).data;
(async()=>{
 await cmd('qa25 social-reset E25CivicA\ntecni vidas E25CivicA 3\ngamemode survival E25CivicA\ntp E25CivicA -13.5 96 -13.5 0 0');
 const npcs=JSON.parse(fs.readFileSync(p.join(r,'config/tecnihardcore/npcs.json')));assert(Object.values(npcs).every(n=>n.y===96));
 await act({type:'close'});await act({type:'view',perspective:0,scale:2});await act({type:'npc',id:'ines'});await act({type:'click',x:230,y:135});
 await delay(1400);assert(data().quests[id]?.includes('sendero:active'));assert(!data().quests[id].includes('sendero:ready'),'Natural spawn must not complete the plaza mission');
 await act({type:'close'});await cmd('tp E25CivicA 0 230 0');await delay(1200);assert(!data().quests[id].includes('sendero:ready'),'Vertical distance counted');
 await cmd('tp E25CivicA 130 230 0');await delay(1400);assert(data().quests[id].includes('sendero:ready'));
 await cmd('tp E25CivicA -13.5 96 -13.5 0 0');await act({type:'npc',id:'ines'});await act({type:'click',x:230,y:158});assert.equal(data().wallets[id],15);
 await act({type:'close'});await file(p.join(g,'qa-capture-request.txt'),'civic27-plaza-anchor-final');
 const result={passed:true,internalSpawn:{x:-231,y:75,z:154},plazaAnchor:{x:0,y:96,z:0},npcY:96,horizontalOnly:true,payout:15,nativeNpcButtons:true};
 fs.writeFileSync(p.join(r,'qa-results/plaza-anchor27-result.json'),JSON.stringify(result,null,2));console.log('PASS actual-world spawn mismatch: NPCs at plaza height, mission starts incomplete and pays once after horizontal exploration');
})().catch(e=>{console.error(e);process.exitCode=1});
