#!/bin/bash

# Build script for Android Auto Lyrics App

echo "🎵 Building Spotify Lyrics for Android Auto..."

# Check if Android SDK is installed
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME not set. Please set your Android SDK path."
    exit 1
fi

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build debug APK
echo "📦 Building debug APK..."
./gradlew assembleDebug

# Install on connected device
echo "📱 Installing on connected device..."
./gradlew installDebug

echo "✅ Build complete! 🎉"
echo ""
echo "📋 Next steps:"
echo "1. Open Spotify on your device"
echo "2. Play any song"
echo "3. Launch the 'Spotify Lyrics' app"
echo "4. For Android Auto testing:"
echo "   - Connect your device to your car or Android Auto"
echo "   - Or use the Android Auto Desktop Head Unit"
echo ""
echo "🎶 Enjoy your synced lyrics!"