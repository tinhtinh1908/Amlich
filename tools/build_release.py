#!/usr/bin/env python3
"""Build an aligned, unsigned release using an installed JDK and Android SDK."""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile
ROOT = Path(__file__).resolve().parents[1]
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--sdk',default=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT'))
    parser.add_argument('--jdk',default=os.environ.get('JAVA_HOME'))
    args = parser.parse_args()
    if not args.sdk or not args.jdk: parser.error('Set ANDROID_HOME/JAVA_HOME or supply --sdk and --jdk')
    sdk, jdk = Path(args.sdk), Path(args.jdk)
    bt = sdk / 'build-tools/34.0.0'; android = sdk / 'platforms/android-34/android.jar'
    config = (ROOT/'build.gradle').read_text()
    version = re.search(r"versionName '([^']+)'",config).group(1)
    code = re.search(r'versionCode (\d+)',config).group(1)
    package = re.search(r"applicationId '([^']+)'",config).group(1)
    out = ROOT / 'build/native-release'
    if out.exists(): shutil.rmtree(out)
    out.mkdir(parents=True)
    gen = out/'gen'; gen.mkdir(); classes=out/'classes'; classes.mkdir(); dex=out/'dex'; dex.mkdir()
    env=dict(os.environ,JAVA_HOME=str(jdk),PATH=str(jdk/'bin')+os.pathsep+os.environ.get('PATH',''))
    def run(*argv): subprocess.run(list(map(str,argv)),check=True,cwd=ROOT,env=env)
    ET.register_namespace('android','http://schemas.android.com/apk/res/android')
    manifest=ET.parse(ROOT/'AndroidManifest.xml');manifest.getroot().set('package',package)
    app=manifest.getroot().find('application');app.set('{http://schemas.android.com/apk/res/android}debuggable','false')
    manifest.write(out/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
    run(bt/'aapt2','compile','--dir',ROOT/'res','-o',out/'resources.zip')
    run(bt/'aapt2','link','-o',out/'base.apk','-I',android,'--manifest',out/'AndroidManifest.xml',
        '--java',gen,'--custom-package',package,'--min-sdk-version','30','--target-sdk-version','34',
        '--version-code',code,'--version-name',version,'-A',ROOT/'assets',out/'resources.zip')
    run(jdk/'bin/javac','-encoding','UTF-8','--release','8','-classpath',android,'-d',classes,
        *sorted((ROOT/'src').rglob('*.java')),*sorted(gen.rglob('*.java')))
    run(jdk/'bin/jar','cf',out/'classes.jar','-C',classes,'.')
    run(bt/'d8','--release','--min-api','30','--lib',android,'--output',dex,out/'classes.jar')
    with zipfile.ZipFile(out/'base.apk','a',zipfile.ZIP_DEFLATED) as archive:
        for f in sorted(dex.glob('*.dex')): archive.write(f,f.name)
    dest=ROOT/'build/outputs/apk/release'/f'AmLichViet-{version}-vc{code}-release-unsigned.apk'
    dest.parent.mkdir(parents=True,exist_ok=True)
    run(bt/'zipalign','-f','-p','4',out/'base.apk',dest)
    run(bt/'zipalign','-c','4',dest)
    print(dest)
if __name__=='__main__':main()
