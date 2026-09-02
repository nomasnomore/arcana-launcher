#!/bin/bash
# CI build — same manual pipeline, but using the runner's Android SDK.
set -e
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
AJ="$SDK/platforms/android-34/android.jar"
echo "build-tools: $BT"; echo "android.jar: $AJ"
rm -rf gen obj build; mkdir -p gen obj build

echo "[1/6] aapt: R.java"
"$BT/aapt" package -f -m -J gen -M AndroidManifest.xml -S res -I "$AJ"

echo "[2/6] javac"
find src gen -name '*.java' > build/sources.txt
javac -encoding UTF-8 -source 8 -target 8 -nowarn -Xlint:none \
  -bootclasspath "$AJ" -classpath "$AJ" -d obj @build/sources.txt

echo "[3/6] d8: dex"
( cd obj && jar cf ../build/classes.jar . )
"$BT/d8" --min-api 26 --lib "$AJ" --output build build/classes.jar

echo "[4/6] aapt: package + add dex"
"$BT/aapt" package -f -M AndroidManifest.xml -S res -A assets -I "$AJ" -F build/unsigned.apk
( cd build && "$BT/aapt" add unsigned.apk classes.dex > /dev/null )

echo "[5/6] zipalign"
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk

echo "[6/6] apksigner"
"$BT/apksigner" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out build/BlueHour.apk build/aligned.apk
"$BT/apksigner" verify --print-certs build/BlueHour.apk | grep -i "SHA-256" | head -1
ls -la build/BlueHour.apk
