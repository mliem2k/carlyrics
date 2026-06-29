# Spotify Lyrics for Android Auto

An Android Auto app that displays synchronized lyrics for the currently playing track on Spotify.

## Features

- **Real-time Lyrics Sync**: Shows lyrics synchronized with Spotify playback
- **Android Auto Support**: Full integration with Android Auto car display
- **Background Service**: Runs in foreground to track Spotify playback
- **Auto-refresh**: Automatically loads lyrics when track changes
- **Offline Support**: Caches lyrics for previously played songs
- **Free Lyrics Sources**: Works without paid APIs! 🎉

## 🆕 FREE Lyrics Sources! (Updated with Latest Tech)

This app now supports multiple FREE lyrics sources - no Spotify API or paid services required:

### 1. **Local LRC Files** (Recommended)
- Download `.lrc` files from lrclib.net
- Smart filename and content matching
- Room database caching for offline access

### 2. **Genius Scraper**
- Automatically fetches lyrics from Genius.com
- Uses latest JSoup library for reliable scraping
- Free and unlimited for personal use

### 3. **Musixmatch Scraper**
- Alternative source when Genius isn't available
- Free web scraping approach
- Fallback option for lyrics

### 🚀 **Latest Technologies Used**:
- **Kotlin 2.1.0** - Latest stable version
- **Android 15 (API 35)** support
- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - Latest design system
- **Hilt** - Modern dependency injection
- **Room Database** - Local persistence
- **Media3** - Latest media framework
- **Coroutines** - Asynchronous programming
- **MVVM Architecture** - Clean architecture pattern

## How It Works

### 1. Spotify Integration
The app uses two methods to track Spotify playback:
- **Broadcast Receiver**: Listens to Spotify's broadcast intents (`com.spotify.music.playbackstatechanged` and `com.spotify.music.metadatachanged`)
- **Media Session Manager**: Monitors Android's media sessions to detect Spotify playback

### 2. FREE Lyrics Provider
- **Local LRC Files**: Import your own synchronized lyrics files
- **Genius Scraper**: Fetches lyrics for free from genius.com
- **Musixmatch Scraper**: Alternative free source
- **Smart Caching**: Stores fetched lyrics for offline use
- **Fallback**: Mock lyrics when no source is available

### 3. Android Auto Integration
- Uses Android Car App Library
- Displays lyrics in a car-safe scrollable list
- Highlights current playing line
- Auto-refreshes with track changes

## 🚀 Getting Free Lyrics

### Method 1: Download LRC Files (Best Quality)
1. Search for "[Song Name] LRC download" on Google
2. Popular sites:
   - [lrclib.net](https://lrclib.net) - Huge collection of LRC files
   - [lyricstime.com](https://lyricstime.com)
   - [makeitpersonal.co](https://makeitpersonal.co)
3. Use the app's Lyrics Manager to import the files

### Method 2: Automatic Fetching
The app automatically tries to fetch lyrics from:
- Genius.com (primary source)
- Musixmatch.com (fallback)

No setup required - just play music!

## Setup Instructions

### Prerequisites
1. Android Studio with Android SDK
2. Android device or emulator with Android 7.0 (API 24) or higher
3. Spotify app installed
4. Android Auto Simulator (for testing)

### Building the App

1. Clone the repository:
```bash
git clone https://github.com/yourusername/android-auto-lyrics.git
cd android-auto-lyrics
```

2. Open in Android Studio

3. Build and run the app:
```bash
./gradlew assembleDebug
```

4. Install on device:
```bash
./gradlew installDebug
```

### Testing on Android Auto

#### Method 1: Desktop Head Unit (DHU)
1. Install Android Auto on your phone
2. Enable developer options in Android Auto
3. Connect phone via USB
4. Run Desktop Head Unit:
```bash
./gradlew connectedDebugAndroidTest
```

#### Method 2: Android Auto Simulator
1. Download Android Auto Simulator
2. Connect your device via ADB
3. Run the simulator

## Key Components

### `SpotifyBroadcastReceiver`
Receives Spotify playback state and metadata changes via broadcast intents.

### `MediaSessionManager`
Monitors system media sessions to detect Spotify playback when broadcasts aren't available.

### `LyricsService`
Foreground service that:
- Manages lyrics synchronization
- Updates based on playback position
- Handles lifecycle events

### `LyricsScreen` (Android Auto)
Main car screen showing:
- Current track info
- Synchronized lyrics list
- Current line highlighting

## Production Considerations

### 1. Spotify API Integration
To use official Spotify API:
- Register app at [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
- Implement OAuth2 authentication
- Use Web API endpoints for track info

### 2. Lyrics Sources
Popular lyrics APIs:
- **Musixmatch**: Official API with timestamps
- **Genius**: Community-edited lyrics
- **ChartLyrics**: Free API
- **Local LRC files**: User-provided synchronized lyrics

### 3. Permissions
Required permissions:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### 4. Limitations
- Spotify doesn't provide official timestamps
- Some Android versions restrict broadcast access
- Lyrics APIs require authentication and have rate limits

## Troubleshooting

### App Not Detecting Spotify
1. Check if Spotify is installed and playing
2. Verify notification access is granted
3. Try restarting both apps

### Android Auto Not Showing App
1. Check Android Auto setup
2. Verify car app service is properly declared
3. Check logcat for CarApp errors

### Lyrics Not Syncing
1. Verify playback position updates
2. Check lyrics format (LRC format recommended)
3. Adjust sync timing in `LyricsService`

## Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## License

This is a demonstration project. Please ensure compliance with:
- Spotify Developer Terms
- Lyrics API terms of service
- Android Auto guidelines

## 🎵 Using the App

1. **Import LRC Files** (Optional but recommended)
   - Open the app
   - Go to Lyrics Manager
   - Import your LRC files
   - Files are automatically matched by Spotify track ID

2. **Play Music on Spotify**
   - The app automatically detects what's playing
   - Lyrics load automatically
   - Works in Android Auto too!

3. **Automatic Fetching**
   - If no LRC file exists, the app tries Genius and Musixmatch
   - Fetched lyrics are cached for offline use

## ⚠️ Legal Notice

This app is for personal use only. By using this app, you agree to:
- Only download lyrics you have rights to use
- Respect copyright and terms of service
- Not use the app for commercial purposes
- Support artists by purchasing music legally

The scrapers are provided for educational purposes. Use responsibly!