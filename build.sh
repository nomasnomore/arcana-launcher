#!/bin/bash
# Blue Hour launcher — manual APK build (no Gradle).
# aapt (resources) -> javac (compile vs android.jar) -> dx (dex) -> aapt add -> zipalign -> apksigner
set -e
cd "$(dirname "$0")"
AJ=/opt/android-jars/android-34/android.jar

rm -rf gen obj build
mkdir -p gen obj build

echo "[1/6] aapt: generate R.java"
aapt package -f -m -J gen -M AndroidManifest.xml -S res -I "$AJ"

echo "[2/6] javac: compile"
find src gen -name '*.java' > build/sources.txt
javac -encoding UTF-8 -source 8 -target 8 -nowarn -Xlint:none \
      -bootclasspath "$AJ" -classpath "$AJ" \
      -d obj @build/sources.txt

echo "[3/6] dx: dex"
dalvik-exchange --dex --min-sdk-version=26 --output=build/classes.dex obj 2>/dev/null

echo "[4/6] aapt: package resources"
aapt package -f -M AndroidManifest.xml -S res -A assets -I "$AJ" -F build/unsigned.apk
( cd build && aapt add unsigned.apk classes.dex > /dev/null )

echo "[5/6] zipalign"
zipalign -f 4 build/unsigned.apk build/aligned.apk

echo "[6/6] sign"
if [ ! -f debug.keystore ]; then
  keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
    -alias bluehour -dname "CN=Blue Hour" -keyalg RSA -keysize 2048 -validity 10000 2>/dev/null
fi
apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out build/BlueHour.apk build/aligned.apk
apksigner verify build/BlueHour.apk && echo "BUILD OK:" && ls -la build/BlueHour.apk
