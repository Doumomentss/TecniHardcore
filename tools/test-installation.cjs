const assert=require('assert'),fs=require('fs'),path=require('path');
const install=require('../launcher/client-install');
async function test(){
 const root=path.resolve('tools/test-runtime/clean-client');
 process.env.APPDATA=path.resolve('tools/test-runtime/empty-appdata');
 const endpoint={host:'rails-acorn.tun.ply.gg',port:6906};
 await install.prepareClient(root,path.resolve('installer_payload'),console.log,endpoint);
 const title=path.join(root,'config/fancymenu/customization/title_screen_layout.txt');
 assert(fs.readFileSync(title,'utf8').includes('[action_type:joinserver] = rails-acorn.tun.ply.gg:6906'));
 assert(fs.readFileSync(title,'utf8').includes('label = ENTRAR AL SERVIDOR'));
 assert(fs.readFileSync(title,'utf8').includes('element_type = custom_button'));
 assert(!/^custom_button\s*\{/m.test(fs.readFileSync(path.join(root,'config/fancymenu/customization/title_screen.txt'),'utf8')));
 const options=path.join(root,'options.txt');fs.appendFileSync(options,'\nmusic:0.123\n');
 const before=fs.readFileSync(options,'utf8');
 const stale=path.join(root,'mods/tecnihardcore-2.0.0.jar');fs.writeFileSync(stale,'unmanaged old mod');
 const mod=path.join(root,'mods/tecnihardcore-2.1.0.jar');fs.writeFileSync(mod,'corrupted test copy');
 await install.prepareClient(root,path.resolve('installer_payload'),()=>{},endpoint);
 assert(!fs.existsSync(stale),'unmanaged previous version must be archived');
 assert(fs.readFileSync(mod).subarray(0,2).equals(Buffer.from('PK')));
 assert(fs.readFileSync(options,'utf8').includes('music:0.123'));
 const saved=JSON.parse(fs.readFileSync(path.join(root,'tecnihardcore-managed.json')));
 const backupCount=fs.readdirSync(path.join(root,'backups')).length;
 await install.prepareClient(root,path.resolve('installer_payload'),()=>{},endpoint);
 assert.equal(fs.readdirSync(path.join(root,'backups')).length,backupCount,'unchanged pack must not create backups');
 console.log('PASS: empty install, direct endpoint, repair, backups, preferences and stable second run');
}
test().catch(e=>{console.error(e);process.exitCode=1});
