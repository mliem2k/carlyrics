#!/bin/bash

echo "🎵 Quick Release Build - Spotify Lyrics"
echo "====================================="

# Set JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home

# Try to find Android SDK in common locations
if [ -d "$HOME/Library/Android/sdk" ]; then
    export ANDROID_HOME="$HOME/Library/Android/sdk"
    echo "✅ Found Android SDK at: $ANDROID_HOME"
else
    echo "⚠️  Android SDK not found. Creating basic local.properties..."
    echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
    echo ""
    echo "Please install Android Studio or Android SDK Command Line Tools"
    echo "and set ANDROID_HOME environment variable."
    echo ""
    echo "For now, building debug APK instead (works the same)..."
    echo ""

    # Build debug instead
    if ./gradlew assembleDebug; then
        echo ""
        echo "✅ Debug APK built successfully!"
        echo "📦 Location: app/build/outputs/apk/debug/app-debug.apk"
        echo ""
        echo "📱 Install with:"
        echo "adb install -r app/build/outputs/apk/debug/app-debug.apk"
        echo ""
        echo "Note: Debug APK works exactly the same as release for testing!"
    else
        echo "❌ Build failed"
    fi

    exit 1
fi

# Add Android tools to PATH
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

# Build release
echo "📦 Building release APK..."
./gradlew assembleRelease

# Check if build was successful
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo ""
    echo "✅ Release APK built successfully!"
    echo "📦 Location: app/build/outputs/apk/release/app-release.apk"
    echo "📱 Size: $(du -h app/build/outputs/apk/release/app-release.apk | cut -f1)"
    echo ""
    echo "📋 To install:"
    echo "adb install -r app/build/outputs/apk/release/app-release.apk"

    # Copy to outputs folder
    mkdir -p build/outputs/apk
    cp app/build/outputs/apk/release/app-release.apk build/outputs/apk/SpotifyLyrics-release.apk
    echo "✅ Also copied to: build/outputs/apk/SpotifyLyrics-release.apk"
else
    echo ""
    echo "❌ Build failed. Trying debug APK..."

    if ./gradlew assembleDebug; then
        echo ""
        echo "✅ Debug APK built successfully!"
        echo "📦 Location: app/build/outputs/apk/debug/app-debug.apk"
        echo "📱 Install with:"
        echo "adb install -r app/build/outputs/apk/debug/app-debug.apk"
    fi
fi

echo ""
echo "🎉 Done!"