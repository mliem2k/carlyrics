#!/bin/bash

# Quick build script - uses Android's default debug keystore

echo "🎵 Quick Build - Spotify Lyrics"
echo "================================"

# Check for gradlew
if [ ! -f "./gradlew" ]; then
    echo "❌ gradlew not found. Are you in the project root?"
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

# Clean and build debug
echo "🧹 Cleaning..."
./gradlew clean

echo "📦 Building debug APK..."
./gradlew assembleDebug

# Check if build was successful
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "📱 APK Location: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "🚀 Install commands:"
    echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "Or run: ./install.sh"
    echo ""
else
    echo ""
    echo "❌ Build failed!"
    echo "Check the error messages above"
fi