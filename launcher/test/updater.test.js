const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('fs'),path=require('path'),os=require('os'),crypto=require('crypto');
const updater=require('../updater');
const bytes=Buffer.from('Verified installer fixture');
function manifest(version='2.1.2') {return {schema:1,version,packVersion:'2.1.0',notes:'Santuarios y actualizaciones.',installer:{url:`https://github.com/${updater.REPOSITORY}/releases/download/v${version}/TecniHardcore-Setup-${version}.exe`,bytes:bytes.length,sha256:crypto.createHash('sha256').update(bytes).digest('hex')}};}
test('semantic versions do not downgrade or confuse 2.1.10 and 2.1.9',()=>{
  assert(updater.newer('2.1.10','2.1.9'));assert(!updater.newer('2.1.1','2.1.1'));assert(!updater.newer('2.0.9','2.1.1'));assert.throws(()=>updater.newer('v2.1.2','2.1.1'));
});
test('manifest cannot choose another repository, executable path or invalid hash',()=>{
  const data=manifest();assert.equal(updater.validateManifest(data),data);
  for(const invalid of [{...data,schema:2},{...data,version:'../../evil'},{...data,installer:{...data.installer,url:'https://example.com/malware.exe'}},{...data,installer:{...data.installer,sha256:'bad'}}])assert.throws(()=>updater.validateManifest(invalid));
});
test('latest release checks update, current version, and useful offline errors',async()=>{
  const fixture=async()=>new Response(JSON.stringify(manifest()));
  assert((await updater.check('2.1.1',fixture)).available);
  assert(!(await updater.check('2.1.2',fixture)).available);
  await assert.rejects(updater.check('2.1.1',async()=>new Response('',{status:404})),/publicada/);
  await assert.rejects(updater.check('2.1.1',async()=>{throw Error('offline');}),/offline/);
});
test('streamed download verifies hash/length and removes partial files after errors',async()=>{
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'tecni-updater-test-'));
  try {
    const data=manifest();let progress=0;
    const downloaded=await updater.download(data,dir,p=>progress=p.percentage,undefined,async()=>new Response(bytes));
    assert.equal(progress,100);assert.deepEqual(fs.readFileSync(downloaded),bytes);
    await assert.rejects(updater.download({...data,installer:{...data.installer,sha256:'0'.repeat(64)}},dir,()=>{},undefined,async()=>new Response(bytes)),/SHA-256/);
    assert(!fs.existsSync(downloaded+'.part'));assert.deepEqual(fs.readFileSync(downloaded),bytes);
    await assert.rejects(updater.download(data,dir,()=>{},undefined,async()=>new Response(Buffer.concat([bytes,bytes]))),/tamaño/);
    assert(!fs.existsSync(downloaded+'.part'));
    const abort=new AbortController();abort.abort();
    await assert.rejects(updater.download(data,dir,()=>{},abort.signal,async()=>new Response(bytes)),/abort/i);
    assert(!fs.existsSync(downloaded+'.part'));
  } finally {fs.rmSync(dir,{recursive:true,force:true});}
});
