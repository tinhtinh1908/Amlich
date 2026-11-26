#!/usr/bin/env python3
"""Build the unsigned release APK through the project's Gradle wrapper."""
import argparse
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sdk", default=os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT"))
    parser.add_argument("--jdk", default=os.environ.get("JAVA_HOME"))
    args = parser.parse_args()

    env = os.environ.copy()
    if args.sdk:
        env["ANDROID_HOME"] = str(Path(args.sdk).resolve())
        env["ANDROID_SDK_ROOT"] = env["ANDROID_HOME"]
    if args.jdk:
        env["JAVA_HOME"] = str(Path(args.jdk).resolve())
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    subprocess.run([str(wrapper), "assembleRelease"], check=True, cwd=ROOT, env=env)
    for apk in (ROOT / "build/outputs/apk/release").glob("*.apk"):
        print(apk)


if __name__ == "__main__":
    main()
