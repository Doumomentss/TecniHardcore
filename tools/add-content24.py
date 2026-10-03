"""Resolve official Fabric 1.20.1 dependencies once; retain pinned lock on future runs.
Only hashes/URLs are distributed. Third-party jars are downloaded by the launcher.
"""
from pathlib import Path
import urllib.request,urllib.parse,json,hashlib,io,zipfile
R=Path(__file__).resolve().parents[1]
LOCK=R/'tools/content24-lock.json'
def get(url):
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'TecniHardcore/2.4.0 (pack compatibility verification)'}),timeout=90) as r:return r.read()
def api(p):return json.loads(get('https://api.modrinth.com/v2/'+p))
catalogPath=R/'installer_payload/mods-downloads.json';catalog=json.loads(catalogPath.read_text('utf8'))
existing={x.get('project') for x in catalog['files']}
queue=['Tq7tcDiO','OtwNg4r4','yDqYTUaf','mGE2GYeN','mBYmRou0']
versions=json.loads(LOCK.read_text('utf8')) if LOCK.exists() else []
if not versions:
 seen=set()
 while queue:
  version=api('version/'+queue.pop(0));project=version['project_id']
  if project in seen:continue
  assert '1.20.1' in version['game_versions'] and 'fabric' in version['loaders']
  seen.add(project);versions.append(version)
  for dependency in version['dependencies']:
   if dependency['dependency_type']!='required' or dependency.get('project_id') in existing|seen:continue
   if dependency.get('version_id'):queue.append(dependency['version_id']);continue
   query=urllib.parse.urlencode({'game_versions':'["1.20.1"]','loaders':'["fabric"]'})
   candidates=api('project/'+dependency['project_id']+'/version?'+query)
   released=[v for v in candidates if v['version_type']=='release'];queue.append((released or candidates)[0]['id'])
 LOCK.write_text(json.dumps(versions,indent=2,ensure_ascii=False),encoding='utf8')
creditsPath=R/'dist/THIRD-PARTY-MODS.json';credits=json.loads(creditsPath.read_text('utf8'))
for version in versions:
 f=next((f for f in version['files'] if f['primary']),version['files'][0]);data=get(f['url'])
 assert hashlib.sha512(data).hexdigest()==f['hashes']['sha512']
 meta=json.loads(zipfile.ZipFile(io.BytesIO(data)).read('fabric.mod.json'))
 for directory in ['client/mods','installer_payload/mods','tools/test-runtime/content24/mods']:
  base=R/directory;base.mkdir(parents=True,exist_ok=True);(base/f['filename']).write_bytes(data)
 # Staging only: production server deployment follows isolated compatibility tests.
 entry=dict(name=f['filename'],url=f['url'],bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),sha512=f['hashes']['sha512'],project=version['project_id'],version=version['id'])
 catalog['files']=[x for x in catalog['files'] if x.get('project')!=version['project_id']]+[entry]
 credits=[x for x in credits if x.get('id')!=meta['id']]+[dict(file=f['filename'],id=meta['id'],name=meta['name'],version=meta['version'],license=meta.get('license'),contact=meta.get('contact',{}),authors=meta.get('authors',[]))]
 print(meta['id'],meta['version'],'depends',json.dumps(meta.get('depends',{})))
catalogPath.write_text(json.dumps(catalog,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
creditsPath.write_text(json.dumps(credits,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
print('Official content downloaded and pinned:',len(versions),'mods; catalog',len(catalog['files']))
