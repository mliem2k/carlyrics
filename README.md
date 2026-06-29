# CarLyrics

Android app that displays real time synchronized lyrics for the currently playing track, with full Android Auto support.

## Features

- Real time lyrics sync via LRCLIB (timestamped, karaoke style)
- Android Auto and Android Automotive support
- Home screen Glance widget showing live lyric
- Companion View: full screen scrolling lyrics with auto-scroll
- Live notification showing the current lyric line
- Lyrics Library: browse cached lyrics, search, import LRC files
- Offline caching via Room database
- No Spotify API or paid services required

## Tech Stack

- Kotlin 2.3.21 with Jetpack Compose
- AGP 9.2.0, Gradle 9.4.1
- Hilt 2.60 for dependency injection
- Room 2.7.0 for local persistence
- Media3 for media session management
- Android Car App Library 1.7.0
- Glance 1.1.1 for home screen widget
- LRCLIB API for synchronized lyrics

## Download

Grab the latest nightly APK from [Releases](https://github.com/mliem2k/carlyrics/releases/latest) and sideload it (Settings, Install unknown apps). Requires Android 11+.

## Building

```bash
git clone https://github.com/mliem2k/carlyrics.git
cd carlyrics
./gradlew assembleDebug
```

Install on a connected device:

```bash
./gradlew installDebug
```

## Nightly Release

Run `./nightly_release.sh` to build a signed release APK and publish it to GitHub Releases. Requires the keystore file and `gh` CLI authenticated.

## How It Works

1. The app listens for Spotify broadcast intents and monitors Android media sessions to detect what is playing.
2. It queries LRCLIB for timestamped lyrics, caches the result in Room, and keeps a pointer to the current line using the playback position.
3. The current lyric line is pushed to the notification, the Glance widget, the Companion View screen, and the Android Auto car screen simultaneously.

## Android Auto Setup

1. Install Android Auto on your phone.
2. Enable developer mode in Android Auto settings.
3. Connect via USB and open the Desktop Head Unit for testing.

## Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## License

MIT. For personal use. Respect copyright and artist rights.
