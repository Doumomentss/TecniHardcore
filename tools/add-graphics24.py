from pathlib import Path
import urllib.request,json,hashlib,io,zipfile
R=Path(__file__).resolve().parents[1]
def get(url):
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'TecniHardcore/2.4.0'}),timeout=90) as r:return r.read()
iris=json.loads(get('https://api.modrinth.com/v2/version/s5eFLITc'));f=iris['files'][0];data=get(f['url'])
assert hashlib.sha512(data).hexdigest()==f['hashes']['sha512']
for base in [R/'client',R/'installer_payload']:(base/'mods'/f['filename']).write_bytes(data)
catalogPath=R/'installer_payload/mods-downloads.json';catalog=json.loads(catalogPath.read_text('utf8'))
record={'name':f['filename'],'url':f['url'],'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'sha512':f['hashes']['sha512']}
catalog['files']=[x for x in catalog['files'] if not x['name'].startswith('iris-')]+[record];catalogPath.write_text(json.dumps(catalog,indent=2)+'\n','utf8')
version=json.loads(get('https://api.modrinth.com/v2/version/Bqen1mJX'));f=version['files'][0];shader=get(f['url']);assert hashlib.sha512(shader).hexdigest()==f['hashes']['sha512']
props=zipfile.ZipFile(io.BytesIO(shader)).read('shaders/shaders.properties').decode()
profiles={}
for name in ['MEDIUM','ULTRA']:
 line=next(l for l in props.splitlines() if l.strip().startswith('profile.'+name+' '));pairs=line.split('=',1)[1].split();profiles[name]=dict(pair.split('=',1) if '=' in pair else (pair.lstrip('!'),'false' if pair.startswith('!') else 'true') for pair in pairs)
manifest={'name':f['filename'],'version':'r5.9.3','url':f['url'],'bytes':len(shader),'sha256':hashlib.sha256(shader).hexdigest(),'sha512':f['hashes']['sha512'],'project':'https://modrinth.com/shader/complementary-reimagined','credit':'Complementary Development / EminGT','profiles':profiles}
(R/'installer_payload/shaders-download.json').write_text(json.dumps(manifest,indent=2)+'\n','utf8')
# The official shader is a local QA download only, never part of the installer.
local=R/'tools/test-runtime/graphics24';local.mkdir(parents=True,exist_ok=True);(local/f['filename']).write_bytes(shader)
print('Pinned Iris 1.7.6 and original Complementary r5.9.3; shader absent from payload.')
