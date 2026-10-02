const fs=require('fs'),path=require('path'),crypto=require('crypto');
const root=path.resolve(__dirname,'..');
(async()=>{
 const files=JSON.parse(fs.readFileSync(path.join(__dirname,'development-dependencies.json'))).files;
 for(const dependency of files){
  const dest=path.resolve(root,dependency.path);
  if(!dest.startsWith(root+path.sep)||!dependency.url.startsWith('https://cdn.modrinth.com/data/'))throw Error('Invalid development dependency');
  if(fs.existsSync(dest)&&crypto.createHash('sha256').update(fs.readFileSync(dest)).digest('hex')===dependency.sha256)continue;
  const response=await fetch(dependency.url);if(!response.ok)throw Error(`Download failed: ${response.status}`);
  const bytes=Buffer.from(await response.arrayBuffer());if(crypto.createHash('sha256').update(bytes).digest('hex')!==dependency.sha256)throw Error('Dependency checksum failed');
  fs.mkdirSync(path.dirname(dest),{recursive:true});fs.writeFileSync(dest,bytes);console.log('Verified:',dependency.path);
 }
})().catch(error=>{console.error(error.message);process.exitCode=1;});
