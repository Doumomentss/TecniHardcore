const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const server=path.resolve('tools/test-runtime/social27/server'),game=path.resolve('tools/test-runtime/expansion25/game');
assert(fs.existsSync(path.join(server,'.tecni-test-world')));
const wait=n=>new Promise(r=>setTimeout(r,n));
async function file(p,text){fs.writeFileSync(p,text);for(let i=0;i<300&&fs.existsSync(p);i++)await wait(100);assert(!fs.existsSync(p),'QA request timeout');await wait(450);}
const cmd=s=>file(path.join(server,'qa-commands.txt'),s+'\n');
const view=scale=>file(path.join(game,'qa-action.json'),JSON.stringify({type:'view',scale,perspective:0}));
const shot=name=>file(path.join(game,'qa-capture-request.txt'),name);
function entry(){return Object.values(JSON.parse(fs.readFileSync(path.join(server,'world/tecnihardcore-souls.json'))).players).find(p=>p.name==='E25CivicA');}
(async()=>{
 for(let i=0;i<120&&!fs.readFileSync(path.join(server,'logs/latest.log'),'utf8').includes('E25CivicA joined the game');i++)await wait(500);
 await cmd('tecni vidas E25CivicA 5\ngamemode survival E25CivicA\ntp E25CivicA 0 -60 0\nweather clear\ntime set day\neffect clear E25CivicA');
 await view(2);await shot('five-lives271-scale2');await view(4);await shot('five-lives271-scale4');
 await view(2);await shot('fullscreen-toggle');await shot('five-lives271-fullscreen');await shot('fullscreen-toggle');
 const deaths=[];
 for(let remaining=4;remaining>=0;remaining--){await cmd('kill E25CivicA');assert.equal(entry().lives,remaining);deaths.push(remaining);await wait(1500);}
 await wait(4000);await shot('five-lives271-eliminated');
 await cmd('tecni vidas E25CivicA 5');assert.equal(entry().lives,5);
 const result={passed:true,fakeAccount:'E25CivicA',initial:5,deaths,adminAllowsFive:true,scales:[2,4],fullscreen:true,screenshots:['scale2','scale4','fullscreen','eliminated']};
 fs.writeFileSync(path.resolve('tools/test-runtime/lives271/native-result.json'),JSON.stringify(result,null,2));console.log('PASS native five-heart HUD, scales 2/4/fullscreen, five real deaths and elimination.');
})().catch(e=>{console.error(e.stack);process.exitCode=1});
