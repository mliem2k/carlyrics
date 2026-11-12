# LiveLyrics

<img src="landing/public/favicon.svg" width="96" alt="LiveLyrics icon" />

Synced lyrics for Android Auto, your Android phone and your Mac. Free, open source, no account.

Website: [livelyrics.mliem.com](https://livelyrics.mliem.com)

## Android

LiveLyrics follows whatever is playing in Spotify, YouTube Music, Apple Music, Tidal and other music apps, and shows the lyrics line by line as they are sung.

- **Android Auto** (experimental): the current line and the next few on the car screen. It has not been tested in a real car yet.
- **Home screen**: lyrics over a backdrop taken from the album cover, with a seek bar and player controls. Tap a line to jump the song to it; scroll ahead, then tap Sync to song to come back.
- **Up next**: when the player shares its queue, a sheet lists it and a tap skips to a song.
- **Library**: every song's lyrics are saved on the phone for offline use. Search them, share one as an `.lrc` file, or import your own.
- **Status bar and widget**: a live lyric line in the notification while music plays, and a home screen widget.
- **Settings**: choose the lyrics source (LRCLIB, Genius, Musixmatch), turn automatic lookup off, and update the app from inside it.

Requires Android 11 or newer.

### How it works

1. A notification listener sees which song is playing and reads its position from the media session. Nothing runs while no music is playing.
2. A lookup sends only the song title and artist to the chosen lyrics source.
3. Synced lyrics are cached in Room and pushed to the home screen, notification, widget and Android Auto screen together.

## macOS

A small popup in the corner of the screen follows the song playing in Spotify desktop.

- Hover the popup for previous, play/pause and next; the controls stay out of the way otherwise.
- Scroll to read ahead, then click Sync to jump back to the current line.
- Resize it down to a single line; it remembers its size and position.
- The menu bar icon shows or hides it.

Requires macOS 13 or newer and the Spotify desktop app. See [macos/README.md](macos/README.md) for install and build details.

## Download

Both apps are on the [Releases](https://github.com/mliem2k/livelyrics/releases) page.

- **Android**: download the newest `livelyrics_nightly` APK and open it, allowing installs from your browser if asked. After the first install it updates itself.
- **macOS**: download the `livelyrics_macos` zip, unzip it and move LiveLyrics.app to Applications. The app is not notarized, so on first launch open System Settings, Privacy & Security, and click Open Anyway.

## Building

### Android

The self-updater is the shared [android-core](https://github.com/mliem2k/android-core) module. Check that repository out beside this one (`../android-core`), or set `ANDROID_CORE_DIR` to its path.

```bash
git clone https://github.com/mliem2k/livelyrics.git
cd livelyrics
./gradlew assembleDebug     # or installDebug for a connected device
```

Kotlin 2.3 with Jetpack Compose, AGP 9.2, Hilt, Room, Media3, the Android for Cars App Library and Glance. Compiles against SDK 37, targets 36, minimum 30.

To test Android Auto, turn on developer mode in Android Auto's settings and use the Desktop Head Unit.

### macOS

```bash
./macos/build-app.sh
open macos/build/LiveLyrics.app
```

Swift and SwiftUI, built with Swift Package Manager into a universal (Intel and Apple silicon) app bundle.

## Releasing

- **Android**: `./nightly_release.sh` builds a signed APK and publishes it as a `nightly-YYYYMMDD-BUILD` prerelease. It needs `gh` signed in and `SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` exported.
- **macOS**: set `CFBundleShortVersionString` in `macos/Info.plist`, then run `./macos/release.sh` to publish a `macos-vX.Y.Z` release. The Android updater ignores these tags.

## License

[MIT](LICENSE). Copyright 2025, 2026 Michael Liem. Lyrics belong to their artists and publishers.
