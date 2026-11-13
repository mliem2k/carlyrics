# LiveLyrics for macOS

LiveLyrics for macOS shows synced lyrics for the current Spotify desktop track in a small popup in the top right corner. Hover the popup for previous, play/pause and next. Scroll to read ahead, then click Sync to jump back to the current line. Drag its edges to resize it; it remembers its size and position. The button beside Quit moves it under the notch instead, as a black bar with the previous, current and next line (on a screen without a notch it hangs from the middle of the menu bar). Clicks pass through the bar and it fades while the pointer is over it. There the menu bar icon opens and closes the full popup; in the corner it shows or hides the popup.

## Install

Download the `livelyrics_macos` zip from [Releases](https://github.com/mliem2k/livelyrics/releases), unzip it and move LiveLyrics.app to Applications. The app is not notarized, so macOS blocks the first launch: open System Settings, Privacy & Security, and click Open Anyway. Requires macOS 13 or newer.

## Build

From the repository root, run:

```sh
./macos/build-app.sh
open macos/build/LiveLyrics.app
```

The app asks macOS for permission to read and control Spotify. Grant that access in System Settings under Privacy & Security, then Automation. Spotify must be installed and running before LiveLyrics can read the current song.

To publish a release, set `CFBundleShortVersionString` in `Info.plist` and run `./macos/release.sh`.

The menu bar icon is the Android app's themed icon and the app icon is the landing page favicon. Both are committed in `Resources/`; run `./macos/make-icons.sh` to regenerate them.

The app sends the current track title and artist to LRCLIB to retrieve matching synced lyrics. LiveLyrics does not run its own account or lyrics service.
