#!/bin/bash

APK_PATH="build/outputs/SpotifyLyrics.apk"

echo "📱 APK File Analysis"
echo "===================="

# Check if file exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at: $APK_PATH"
    exit 1
fi

# File info
echo "📍 Location: $APK_PATH"
echo "📊 Size: $(du -h $APK_PATH | cut -f1)"
echo "📋 File type: $(file $APK_PATH)"

# Check if it's a valid ZIP
if unzip -t $APK_PATH >/dev/null 2>&1; then
    echo "✅ ZIP archive: Valid"
else
    echo "❌ ZIP archive: Invalid"
fi

# Check AndroidManifest.xml
if unzip -l $APK_PATH | grep -q "AndroidManifest.xml"; then
    echo "✅ AndroidManifest.xml: Present"
else
    echo "❌ AndroidManifest.xml: Missing"
fi

# Check classes.dex
if unzip -l $APK_PATH | grep -q "classes.dex"; then
    echo "✅ classes.dex: Present"
else
    echo "❌ classes.dex: Missing"
fi

# Check signing
if unzip -l $APK_PATH | grep -q "META-INF/MANIFEST.MF"; then
    echo "✅ Signature: Present"
    jarsigner -verify $APK_PATH >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ Signature: Valid"
    else
        echo "⚠️ Signature: Self-signed (expected)"
    fi
else
    echo "❌ Signature: Missing"
fi

# MIME type check
MIME=$(file -I $APK_PATH 2>/dev/null | cut -d';' -f1)
echo "🏷️ MIME type: $MIME"

echo ""
echo "📋 Installation command:"
echo "adb install -r $APK_PATH"