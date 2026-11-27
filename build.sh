#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_HOME:-/Users/karrynguyen/Library/Android/sdk}"
BT="${SDK}/build-tools/37.0.0"
AJAR="${SDK}/platforms/android-37.2/android.jar"

rm -rf "${DIR}/build"
mkdir -p "${DIR}/build/classes" "${DIR}/build/dex" "${DIR}/build/res"

"${BT}/aapt2" compile --dir "${DIR}/res" -o "${DIR}/build/res/res.zip"
"${BT}/aapt2" link -o "${DIR}/build/unsigned.apk" -I "${AJAR}" \
  --manifest "${DIR}/AndroidManifest.xml" -R "${DIR}/build/res/res.zip" --auto-add-overlay

javac --release 11 -cp "${AJAR}" -d "${DIR}/build/classes" \
  $(find "${DIR}/stubs" "${DIR}/src" -name "*.java")

"${BT}/d8" --min-api 30 --output "${DIR}/build/dex" \
  $(find "${DIR}/build/classes/com" -name "*.class")

(cd "${DIR}/build/dex" && zip -j ../unsigned.apk classes.dex)
(cd "${DIR}" && zip -r "${DIR}/build/unsigned.apk" assets)

if [ ! -f "${DIR}/debug.keystore" ]; then
  keytool -genkeypair -v -keystore "${DIR}/debug.keystore" \
    -storepass android -alias androiddebugkey -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

"${BT}/apksigner" sign --ks "${DIR}/debug.keystore" --ks-pass pass:android \
  --out "${DIR}/RomFeatureHide.apk" "${DIR}/build/unsigned.apk"

ls -lh "${DIR}/RomFeatureHide.apk"
