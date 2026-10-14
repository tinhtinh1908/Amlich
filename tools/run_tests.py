#!/usr/bin/env python3
"""Run pure Java calendar/layout/motion regressions without an Android emulator."""
import os
from pathlib import Path
import shutil
import subprocess
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build' / 'native-tests'
def main():
    OUT.mkdir(parents=True, exist_ok=True)
    java_home = os.environ.get('JAVA_HOME')
    javac = str(Path(java_home) / 'bin/javac') if java_home else shutil.which('javac')
    java = str(Path(java_home) / 'bin/java') if java_home else shutil.which('java')
    if not javac or not java: raise SystemExit('Install JDK 17 or set JAVA_HOME')
    # Motion uses only this platform interface. The tiny test adapter is never packaged.
    adapter = OUT / 'support/android/animation/TimeInterpolator.java'
    adapter.parent.mkdir(parents=True, exist_ok=True)
    adapter.write_text('package android.animation; public interface TimeInterpolator { float getInterpolation(float input); }\n')
    names = ['LunarCalendar','HolidayUtil','TraditionalCalendar','MinimalLayout','MiuixMetrics','MiuixMotion','PaperTearGeometry']
    tests = sorted((ROOT / 'tests').glob('*Test.java'))
    sources = [ROOT / 'src/com/dtinh/lichviet' / (n + '.java') for n in names]
    subprocess.run([javac,'-encoding','UTF-8','--release','8','-d',str(OUT),str(adapter),*map(str,sources),*map(str,tests)],check=True)
    for test in tests:
        package = 'com.dtinh.lichviet.' if 'package com.dtinh.lichviet;' in test.read_text() else ''
        subprocess.run([java,'-cp',str(OUT),package+test.stem],check=True)
if __name__ == '__main__': main()
