#!/bin/bash

echo "🎵 Building Spotify Lyrics APK"
echo "================================"

# Set environment variables
export ANDROID_HOME=${ANDROID_HOME:-"$HOME/Library/Android/sdk"}
export JAVA_HOME=${JAVA_HOME:-"/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"}

# Add tools to PATH
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:$PATH"

echo "Using ANDROID_HOME: $ANDROID_HOME"
echo "Using JAVA_HOME: $JAVA_HOME"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build the APK
echo "📦 Building APK..."
./gradlew assembleRelease

# Check if build was successful
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo ""
    echo "✅ Build successful!"

    # Copy APK to a more convenient location
    mkdir -p build/outputs
    cp app/build/outputs/apk/release/app-release.apk build/outputs/SpotifyLyrics.apk

    # Get file info
    APK_SIZE=$(du -h build/outputs/SpotifyLyrics.apk | cut -f1)

    echo ""
    echo "📱 APK Details:"
    echo "Location: build/outputs/SpotifyLyrics.apk"
    echo "Size: $APK_SIZE"
    echo ""
    echo "📋 To install:"
    echo "adb install -r build/outputs/SpotifyLyrics.apk"
    echo ""
    echo "📋 To verify signature:"
    echo "jarsigner -verify build/outputs/SpotifyLyrics.apk"

    # Show file type
    FILE_TYPE=$(file build/outputs/SpotifyLyrics.apk)
    echo ""
    echo "🔍 File type: $FILE_TYPE"
else
    echo ""
    echo "❌ Build failed!"
    echo "Please check the error messages above."
    exit 1
fi