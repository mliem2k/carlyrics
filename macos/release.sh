#!/usr/bin/env bash
set -euo pipefail

REPO="mliem2k/livelyrics"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$APP_DIR/Info.plist")
# The Android updater only reads vX.Y.Z and nightly tags with an .apk asset, so this tag never reaches it.
TAG="macos-v${VERSION}"
ZIP="$APP_DIR/build/livelyrics_macos_${VERSION}.zip"

"$APP_DIR/build-app.sh"
ditto -c -k --keepParent "$APP_DIR/build/LiveLyrics.app" "$ZIP"
echo "==> Zip ready: ${ZIP} ($(du -h "$ZIP" | cut -f1))"

gh release create "$TAG" "$ZIP" \
  --repo "$REPO" \
  --title "LiveLyrics for macOS ${VERSION}" \
  --prerelease \
  --notes "LiveLyrics for macOS ${VERSION}

A corner popup that shows synced lyrics for the song playing in Spotify desktop.

- Synced LRCLIB lyrics that scroll with the song, with a Sync button after you scroll away
- Previous, play/pause and next controls that appear when you hover the popup
- Resizable; it remembers its size and position
- Can hang under the notch, showing three lines and opening on hover
- Menu bar icon to show or hide it

Install: unzip, move LiveLyrics.app to Applications and open it. The app is not notarized, so macOS blocks the first launch: open System Settings, Privacy & Security, and click Open Anyway. Allow Spotify access when asked.

Minimum: macOS 13"

echo "==> Done: https://github.com/${REPO}/releases/tag/${TAG}"
