"""Pin and download official 1.20.1 Fabric files into staging only, never a live game."""
from pathlib import Path
import json,urllib.request,hashlib,zipfile,io
ROOT=Path(__file__).resolve().parents[1]
IDS=['GW1xcaGA','rhE8MT9Z','NRw0CDAc','pwyEaKDs','J1S3aA8i','UOdaYbhh','mXJWSwbJ']
def get(url):
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'TecniHardcore/2.5.0 compatibility staging'}),timeout=90) as response:return response.read()
lock=ROOT/'tools/content25-lock.json'
versions=json.loads(lock.read_text('utf8')) if lock.exists() else [json.loads(get('https://api.modrinth.com/v2/version/'+id)) for id in IDS]
assert len(versions)==7
lock.write_text(json.dumps(versions,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
catalogpath=ROOT/'installer_payload/mods-downloads.json';catalog=json.loads(catalogpath.read_text('utf8'))
creditpath=ROOT/'dist/THIRD-PARTY-MODS.json';credits=json.loads(creditpath.read_text('utf8'))
for version in versions:
 assert 'fabric' in version['loaders'] and '1.20.1' in version['game_versions']
 f=next(f for f in version['files'] if f['primary']);data=get(f['url']);assert len(data)==f['size'];assert hashlib.sha512(data).hexdigest()==f['hashes']['sha512']
 metadata=json.loads(zipfile.ZipFile(io.BytesIO(data)).read('fabric.mod.json'))
 for directory in ['installer_payload/mods','tools/test-runtime/expansion25/content']:
  target=ROOT/directory;target.mkdir(parents=True,exist_ok=True);(target/f['filename']).write_bytes(data)
 catalog['files']=[e for e in catalog['files'] if e.get('project')!=version['project_id']]
 catalog['files'].append(dict(name=f['filename'],url=f['url'],bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),sha512=f['hashes']['sha512'],project=version['project_id'],version=version['id']))
 credits=[e for e in credits if e.get('id')!=metadata['id']]+[dict(file=f['filename'],id=metadata['id'],name=metadata['name'],version=metadata['version'],license=metadata.get('license'),contact=metadata.get('contact',{}),authors=metadata.get('authors',[]))]
 print(metadata['id'],metadata['version'],json.dumps(metadata.get('depends',{})),flush=True)
catalogpath.write_text(json.dumps(catalog,indent=2,ensure_ascii=False)+'\n',encoding='utf8');creditpath.write_text(json.dumps(credits,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
