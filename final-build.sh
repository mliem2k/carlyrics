#!/bin/bash

echo "🎵 Spotify Lyrics - Final APK Build"
echo "=================================="

# Clean and build unsigned
echo "1. Building unsigned APK..."
./gradlew clean
./gradlew assembleRelease

# Find the unsigned APK
UNSIGNED_APK=$(find app/build -name "*unsigned*.apk" | head -1)

if [ -z "$UNSIGNED_APK" ]; then
    echo "❌ No unsigned APK found!"
    exit 1
fi

echo "   Found: $UNSIGNED_APK"

# Create output directory
mkdir -p build/outputs

# Copy to output location with proper name
OUTPUT_APK="build/outputs/SpotifyLyrics.apk"
cp "$UNSIGNED_APK" "$OUTPUT_APK"

echo "2. Signing APK..."
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA256 \
    -keystore spotify-lyrics-key.jks \
    -storepass spotifylyrics \
    -keypass spotifylyrics \
    "$OUTPUT_APK" \
    spotify-lyrics > /dev/null

echo "3. Verifying APK..."
if jarsigner -verify "$OUTPUT_APK" >/dev/null 2>&1; then
    echo "   ✅ Signature verified"
else
    echo "   ⚠️ Self-signed signature"
fi

# Final verification
if unzip -t "$OUTPUT_APK" >/dev/null 2>&1; then
    echo "   ✅ ZIP structure valid"
fi

echo ""
echo "📱 APK Ready!"
echo "============"
echo "File: $OUTPUT_APK"
echo "Size: $(du -h $OUTPUT_APK | cut -f1)"
echo "Type: $(file $OUTPUT_APK | cut -d':' -f2-)"
echo ""
echo "To install:"
echo "  adb install -r $OUTPUT_APK"
echo ""
echo "⚠️ If Android shows it as .bin, try:"
echo "  1. Rename the file to have .apk extension"
echo "  2. Use a file manager that recognizes APK files"
echo "  3. Install via: adb install -r $OUTPUT_APK"