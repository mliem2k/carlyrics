#!/bin/bash

echo "🎵 Building Flutter Spotify Lyrics Release APK"
echo "=========================================="

# Check if Flutter is available
if ! command -v flutter &> /dev/null; then
    echo "❌ Flutter not found!"
    echo "Please install Flutter SDK first:"
    echo "https://docs.flutter.dev/get-started/install"
    exit 1
fi

cd spotify_lyrics_flutter

echo "📦 Getting dependencies..."
flutter pub get

echo "🔧 Building APK..."
flutter build apk --release --target-platform android-arm64

if [ -f "build/app/outputs/flutter-apk/app-release.apk" ]; then
    cp build/app/outputs/flutter-apk/app-release.apk ../build/outputs/SpotifyLyrics-Flutter.apk
    echo "✅ Flutter APK created at: build/outputs/SpotifyLyrics-Flutter.apk"
    echo "📱 Size: $(du -h ../build/outputs/SpotifyLyrics-Flutter.apk | cut -f1)"
else
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "📋 Installation:"
echo "adb install -r build/outputs/SpotifyLyrics-Flutter.apk"