#!/usr/bin/env python3
"""Build a small native Android APK with SDK Build Tools (no Gradle required)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
parser = argparse.ArgumentParser()
parser.add_argument('--build-tools', type=Path, required=True)
parser.add_argument('--android-jar', type=Path, required=True)
parser.add_argument('--ecj', type=Path, help='Optional Eclipse compiler jar; otherwise use javac.')
args = parser.parse_args()
args.build_tools = args.build_tools.resolve()
args.android_jar = args.android_jar.resolve()
if args.ecj: args.ecj = args.ecj.resolve()
build = ROOT / 'build'
if build.exists(): shutil.rmtree(build)
for name in ['classes', 'dex', 'generated', 'tests']:
    (build / name).mkdir(parents=True, exist_ok=True)

def run(*command, capture=False):
    command = [str(item) for item in command]
    print('Running', Path(command[0]).name, flush=True)
    result = subprocess.run(command, check=True, text=True, capture_output=capture)
    return result.stdout if capture else ''

compiler = ['java', '-jar', str(args.ecj), '-1.8', '-nowarn'] if args.ecj else ['javac', '-source', '8', '-target', '8', '-Xlint:-options']
run(args.build_tools / 'aapt2', 'compile', '--dir', ROOT / 'res', '-o', build / 'resources.zip')
run(args.build_tools / 'aapt2', 'link', '-o', build / 'resources.apk',
    '--manifest', ROOT / 'AndroidManifest.xml', '-I', args.android_jar,
    '--java', build / 'generated', build / 'resources.zip')
sources = sorted((ROOT / 'src').rglob('*.java')) + sorted((build / 'generated').rglob('*.java'))
run(*compiler, '-encoding', 'UTF-8', '-classpath', args.android_jar, '-d', build / 'classes', *sources)

test_sources = sorted((ROOT / 'tests').rglob('*.java'))
if test_sources:
    run(*compiler, '-encoding', 'UTF-8', '-classpath', os.pathsep.join([str(build / 'classes'), str(args.android_jar)]),
        '-d', build / 'tests', *test_sources)
    tests = run('java', '-cp', os.pathsep.join([str(build / 'tests'), str(build / 'classes')]),
        'tw.angus.localeinspector.LanguageUtilTest', capture=True)
    (build / 'test-results.txt').write_text(tests)
    print(tests, end='', flush=True)

classes = sorted((build / 'classes').rglob('*.class'))
run(args.build_tools / 'd8', '--release', '--lib', args.android_jar, '--min-api', '24', '--output', build / 'dex', *classes)
unaligned = build / 'unsigned-unaligned.apk'
with zipfile.ZipFile(build / 'resources.apk') as resources, zipfile.ZipFile(unaligned, 'w') as apk:
    for info in resources.infolist():
        apk.writestr(info, resources.read(info.filename))
    for dex in sorted((build / 'dex').glob('*.dex')):
        apk.write(dex, dex.name, compress_type=zipfile.ZIP_DEFLATED)
run(args.build_tools / 'zipalign', '-f', '4', unaligned, build / 'unsigned.apk')

signing = ROOT / 'signing' / 'locale-inspector-debug.keystore'
signing.parent.mkdir(exist_ok=True)
if not signing.exists():
    run('keytool', '-genkeypair', '-keystore', signing, '-storetype', 'PKCS12',
        '-storepass', 'android', '-keypass', 'android', '-alias', 'locale-inspector',
        '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
        '-dname', 'CN=Locale Inspector Development, OU=Diagnostic Tool')
output = ROOT / 'dist' / 'Locale-Inspector-1.0.0.apk'
output.parent.mkdir(exist_ok=True)
run(args.build_tools / 'apksigner', 'sign', '--ks', signing, '--ks-key-alias', 'locale-inspector',
    '--ks-pass', 'pass:android', '--key-pass', 'pass:android',
    '--v1-signing-enabled', 'true', '--v2-signing-enabled', 'true', '--v3-signing-enabled', 'true',
    '--v4-signing-enabled', 'false', '--out', output, build / 'unsigned.apk')
verification = run(args.build_tools / 'apksigner', 'verify', '--verbose', '--print-certs', output, capture=True)
(build / 'signature-verification.txt').write_text(verification)
print(verification, end='', flush=True)
alignment = run(args.build_tools / 'zipalign', '-c', '-v', '4', output, capture=True)
(build / 'alignment-verification.txt').write_text(alignment)
badging = run(args.build_tools / 'aapt2', 'dump', 'badging', output, capture=True)
(build / 'apk-badging.txt').write_text(badging)
summary = {'file': output.name, 'size_bytes': output.stat().st_size,
    'sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
    'min_sdk': 24, 'target_sdk': 34, 'native_libraries': False,
    'runtime_device_test': 'Not performed; no Android device or emulator available.'}
(output.parent / 'build-summary.json').write_text(json.dumps(summary, indent=2))
print(json.dumps(summary, indent=2))
