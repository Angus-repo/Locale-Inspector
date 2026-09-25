import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent
TOOLS = ROOT / 'toolchain'
TOOLS.mkdir(parents=True, exist_ok=True)
BASE = 'https://dl.google.com/android/repository/'
with urllib.request.urlopen(BASE + 'repository2-1.xml', timeout=45) as response:
    metadata = response.read()
(TOOLS / 'repository.xml').write_bytes(metadata)
tree = ET.fromstring(metadata)
packages = {e.attrib.get('path'): e for e in tree if e.tag.endswith('remotePackage')}
jobs = []
for package, destination in [('build-tools;35.0.0', 'build-tools'), ('platforms;android-35', 'platform')]:
    element = packages[package]
    for archive in element.findall('./archives/archive'):
        if archive.findtext('host-os') not in (None, 'linux'):
            continue
        item = archive.find('complete')
        jobs.append({'package': package, 'url': BASE + item.findtext('url'), 'sha1': item.findtext('checksum'), 'destination': destination})
        break
jobs.append({'package': 'ecj-3.33.0', 'url': 'https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.33.0/ecj-3.33.0.jar', 'destination': 'ecj.jar'})

def obtain(job):
    target = TOOLS / (job['destination'] + ('.zip' if 'sha1' in job else ''))
    with urllib.request.urlopen(job['url'], timeout=90) as response:
        data = response.read()
    if 'sha1' in job:
        assert hashlib.sha1(data).hexdigest() == job['sha1'], job['package']
    target.write_bytes(data)
    if target.suffix == '.zip':
        with zipfile.ZipFile(target) as archive:
            for entry in archive.infolist():
                path = Path(archive.extract(entry, TOOLS / job['destination']))
                permissions = entry.external_attr >> 16
                if permissions:
                    path.chmod(permissions)
    print(json.dumps({'package': job['package'], 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}), flush=True)

with concurrent.futures.ThreadPoolExecutor(max_workers=3) as executor:
    list(executor.map(obtain, jobs))
(TOOLS / 'downloads.json').write_text(json.dumps(jobs, indent=2))
print('Toolchain ready', flush=True)
