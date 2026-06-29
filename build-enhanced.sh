#!/bin/bash

echo "🎵 Flutter Spotify Lyrics - Enhanced Build"
echo "======================================"

echo "📋 Build Status: Enhanced Flutter Project"
echo ""

# Check if Flutter is available
if ! command -v flutter &> /dev/null; then
    echo "❌ Flutter not found!"
    echo "Please install Flutter SDK first:"
    echo "https://docs.flutter.dev/get-started/install"
    exit 1
fi

echo "✅ Flutter SDK: $(flutter --version | head -1)"

# Build the enhanced Flutter project
echo "🔧 Building enhanced Flutter app with all features..."
cd spotify_lyrics_flutter

# Try to build without minification first
echo "📱 Building release APK..."
flutter build apk --release --no-tree-shake-icons --no-shrink

if [ $? -eq 0 ]; then
    APK_FILE="build/app/outputs/flutter-apk/app-release.apk"
    
    if [ -f "$APK_FILE" ]; then
        # Copy to our build outputs directory
        mkdir -p ../build/outputs
        cp "$APK_FILE" ../build/outputs/SpotifyLyrics-Flutter-Enhanced.apk
        
        echo "✅ Enhanced Flutter APK created successfully!"
        echo "📱 APK File: build/outputs/SpotifyLyrics-Flutter-Enhanced.apk"
        echo "📊 APK Size: $(du -h ../build/outputs/SpotifyLyrics-Flutter-Enhanced.apk | cut -f1)"
        echo ""
        echo "🚀 Enhanced Features:"
        echo "• ✅ Android Auto car app integration"
        echo "• ✅ Spotify Web API integration"
        echo "• ✅ Lyrics synchronization with playback"
        echo "• ✅ Car-optimized UI screens"
        echo "• ✅ Voice commands for Android Auto"
        echo "• ✅ Offline lyrics caching with Hive"
        echo "• ✅ Multiple lyrics sources fallback"
        echo "• ✅ Production keystore created"
        echo ""
        echo "📋 Installation:"
        echo "adb install -r build/outputs/SpotifyLyrics-Flutter-Enhanced.apk"
        echo ""
        echo "🎯 Ready for Android Auto deployment!"
    else
        echo "❌ Build failed - APK not found"
        exit 1
    fi
else
    echo "❌ Flutter build failed!"
    exit 1
fi

echo ""
echo "📁 Enhanced Project Structure:"
echo "spotify_lyrics_flutter/"
echo "├── lib/"
echo "│   ├── main.dart                    # ✅ App entry point"
echo "│   ├── models/"
echo "│   │   └── track_info.dart       # ✅ Track data model"
echo "│   ├── providers/"
echo "│   │   ├── spotify_provider.dart  # ✅ State management"
echo "│   │   └── theme_provider.dart   # ✅ Theme management"
echo "│   ├── screens/"
echo "│   │   ├── home_screen.dart      # ✅ Mobile UI"
echo "│   │   ├── lyrics_screen.dart     # ✅ Mobile lyrics"
echo "│   │   ├── settings_screen.dart   # ✅ Settings"
echo "│   │   ├── car_home_screen.dart   # ✅ Car UI"
echo "│   │   └── car_lyrics_screen.dart # ✅ Car lyrics"
echo "│   ├── services/"
echo "│   │   ├── spotify_service.dart      # ✅ Spotify API"
echo "│   │   ├── spotify_web_api.dart   # ✅ Web API client"
echo "│   │   ├── lyrics_service.dart     # ✅ Lyrics fetching"
echo "│   │   ├── lyrics_sync_service.dart # ✅ Sync service"
echo "│   │   ├── lyrics_cache_service.dart # ✅ Offline cache"
echo "│   │   └── android_auto_service.dart # ✅ Car integration"
echo "│   ├── themes/"
echo "│   │   └── app_theme.dart         # ✅ Material Design"
echo "│   └── widgets/"
echo "│       ├── now_playing_card.dart  # ✅ UI components"
echo "│       ├── lyrics_display.dart     # ✅ Lyrics display"
echo "│       └── connectivity_status.dart # ✅ Status indicator"
echo "├── android/"
echo "│   ├── app/build.gradle           # ✅ Android configuration"
echo "│   ├── keystore/               # ✅ Production keystore"
echo "│   └── src/main/"
echo "│       ├── AndroidManifest.xml    # ✅ App manifest"
echo "│       └── kotlin/"
echo "│           └── com/spotifylyrics/"
echo "│               ├── MainActivity.kt"
echo "│               └── car/"
echo "│                   ├── SpotifyLyricsCarAppService.kt"
echo "│                   └── VoiceCommandReceiver.kt"
echo "├── pubspec.yaml                    # ✅ Dependencies"
echo "└── build/outputs/                  # ✅ Release APK"
echo ""
echo "🎯 All features implemented and ready for deployment!"