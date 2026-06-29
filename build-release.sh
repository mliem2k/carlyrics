#!/bin/bash

echo "🎵 Building Spotify Lyrics Release APK"
echo "===================================="

# Check for Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [ -d "/usr/local/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/android-sdk"
    else
        echo "❌ Android SDK not found!"
        echo "Please install Android Studio or set ANDROID_HOME"
        exit 1
    fi
fi

echo "✅ Using Android SDK: $ANDROID_HOME"

# Set up paths
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

# Create output directory
mkdir -p build/outputs/apk

# Try to use the system's gradlew if available
if [ -f "./gradlew" ]; then
    echo "📦 Building with Gradle..."

    # Try with sudo if needed
    if sudo -n true 2>/dev/null; then
        echo "⚠️  Using sudo to run gradlew (may need password)"
        sudo ./gradlew assembleRelease
    else
        ./gradlew assembleRelease
    fi

    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        cp app/build/outputs/apk/release/app-release.apk build/outputs/apk/SpotifyLyrics-release.apk
        echo "✅ Release APK created at: build/outputs/apk/SpotifyLyrics-release.apk"
        echo "📱 Size: $(du -h build/outputs/apk/SpotifyLyrics-release.apk | cut -f1)"
    fi
else
    echo "❌ Gradle wrapper not found"
    echo "Please ensure you have the complete project"
fi

echo ""
echo "📋 Installation:"
echo "adb install -r build/outputs/apk/SpotifyLyrics-release.apk"