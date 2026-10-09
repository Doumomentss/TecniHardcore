const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const s=path.resolve('tools/test-runtime/social27/server'),g=path.resolve('tools/test-runtime/expansion25/game');
const wait=ms=>new Promise(r=>setTimeout(r,ms));
async function req(p,v){fs.writeFileSync(p,v);for(let n=0;n<200&&fs.existsSync(p);n++)await wait(100);assert(!fs.existsSync(p),'timeout '+p);await wait(300);}
const cmd=v=>req(path.join(s,'qa-commands.txt'),v+'\n'),action=v=>req(path.join(g,'qa-action.json'),JSON.stringify(v));
const shot=tag=>req(path.join(g,'qa-capture-request.txt'),tag);
(async()=>{
 await cmd('tecni vidas E25CivicA 5\ngamemode survival E25CivicA\neffect clear E25CivicA\ntp E25CivicA 30 -60 30\nqa25 social-report E25CivicA before-view');
 await action({type:'command',command:'tecni replays ver 6'});await wait(7000);await shot('moderation28-replay');
 await action({type:'command',command:'replay view close'});await wait(2000);await cmd('qa25 social-report E25CivicA after-view');
 let before=JSON.parse(fs.readFileSync(path.join(s,'qa-results/E25CivicA-before-view.json'))),after=JSON.parse(fs.readFileSync(path.join(s,'qa-results/E25CivicA-after-view.json')));assert.equal(after.inventory,before.inventory);assert.equal(after.health,before.health);console.log('PASS native viewer preserves moderator inventory and health');
 fs.writeFileSync(path.resolve('tools/test-runtime/moderation28/viewer-result.json'),JSON.stringify({passed:true,before,after},null,2));
})();
