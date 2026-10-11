#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${MFA_ENCRYPTION_KEY:?Set MFA_ENCRYPTION_KEY before running tests}"
(
  cd mobile
  if test -f package-lock.json; then npm ci; else npm install --no-audit --no-fund; fi
  npm run build
)
./gradlew --no-daemon clean test bootJar
printf '\nBackend: build/libs/*.jar\nAngular app: mobile/dist/web/browser/\n'
if [[ "${BUILD_ANDROID:-false}" == true ]]; then
  cd mobile
  npm run android:init
  (cd android && ./gradlew assembleDebug)
  echo 'Android debug APK: mobile/android/app/build/outputs/apk/debug/app-debug.apk'
fi
