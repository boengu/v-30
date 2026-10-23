#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo 'HUD LINK NAVER V30 2.6 · JDK 17 / Gradle 8.7 / Android SDK 35'
if [ -f ./gradlew ]; then
  bash ./gradlew --no-daemon assembleDebug
elif command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon assembleDebug
else
  echo '[NOT BUILT] Gradle not installed. See README_한국어.md.' >&2
  exit 1
fi
echo 'APK: app/build/outputs/apk/debug/app-debug.apk'
