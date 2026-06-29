#!/usr/bin/env bash
set -euo pipefail

REPO="mliem2k/carlyrics"

export SIGNING_KEYSTORE_PATH="$(pwd)/spotify-lyrics-key.jks"
export SIGNING_STORE_PASSWORD="spotify_lyrics_2024"
export SIGNING_KEY_ALIAS="spotify_lyrics"
export SIGNING_KEY_PASSWORD="spotify_lyrics_2024"

DATE=$(date -u +%Y-%m-%d)
TAG="nightly-$DATE"
APK_SRC="app/build/outputs/apk/release/app-release.apk"
APK_OUT="carlyrics-nightly-$DATE.apk"

echo "==> Building release APK ($TAG)"
./gradlew assembleRelease \
  -PversionName="nightly-$DATE" \
  --quiet

[[ -f "$APK_SRC" ]] || { echo "ERROR: APK not found at $APK_SRC" >&2; exit 1; }
cp "$APK_SRC" "$APK_OUT"
echo "==> APK ready: $APK_OUT ($(du -h "$APK_OUT" | cut -f1))"

echo "==> Publishing GitHub release $TAG"
gh release delete "$TAG" --yes 2>/dev/null || true
gh release create "$TAG" "$APK_OUT" \
  --repo "$REPO" \
  --title "CarLyrics nightly $DATE" \
  --notes "**Nightly build — $DATE**

### What's in this build
- LRCLIB synced lyrics (timestamped, karaoke-style)
- Android Auto / Android Automotive support
- Home screen widget with live lyric
- Lyrics Library: browse cached lyrics, import LRC files
- Companion View: full-screen synced lyrics with auto-scroll
- Live notification: shows current lyric line

### Install
Download \`$APK_OUT\` and sideload (Settings → Install unknown apps).

> **Minimum:** Android 11 (API 30)" \
  --prerelease

echo "==> Done → https://github.com/$REPO/releases/tag/$TAG"
rm -f "$APK_OUT"
