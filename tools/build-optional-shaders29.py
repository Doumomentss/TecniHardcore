"""Pin optional shader downloads from Modrinth; never bundle their ZIPs."""
import hashlib
import json
import urllib.request
from pathlib import Path

IDS = [('BSL', 'Y68yiql9'), ('MakeUp Ultra Fast', 'pgb0JjoN'), ('Complementary Unbound', 'B1kyfoUZ')]
result = []
for title, version_id in IDS:
    with urllib.request.urlopen(f'https://api.modrinth.com/v2/version/{version_id}', timeout=30) as response:
        version = json.load(response)
    assert '1.20.1' in version['game_versions'] and 'iris' in version['loaders']
    file = next(f for f in version['files'] if f['primary'])
    assert file['url'].startswith('https://cdn.modrinth.com/data/') and file['filename'].endswith('.zip')
    with urllib.request.urlopen(file['url'], timeout=60) as response:
        data = response.read()
    assert len(data) == file['size'] and hashlib.sha512(data).hexdigest() == file['hashes']['sha512']
    result.append({'title': title, 'version': version['version_number'], 'name': file['filename'],
                   'url': file['url'], 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
                   'sha512': file['hashes']['sha512']})
target = Path(__file__).resolve().parents[1] / 'installer_payload' / 'optional-shaders.json'
target.write_text(json.dumps({'files': result}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(target, [(v['title'], v['version'], v['bytes']) for v in result])
