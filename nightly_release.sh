#!/usr/bin/env bash
set -euo pipefail

REPO="mliem2k/carlyrics"

export SIGNING_KEYSTORE_PATH="$(pwd)/spotify_lyrics_key.jks"
export SIGNING_STORE_PASSWORD="spotifylyrics"
export SIGNING_KEY_ALIAS="spotify-lyrics"
export SIGNING_KEY_PASSWORD="spotifylyrics"

DATE=$(date -u +%Y%m%d)
TAG="nightly_${DATE}"
APK_SRC="app/build/outputs/apk/release/app-release.apk"
APK_OUT="carlyrics_nightly_${DATE}.apk"

echo "==> Building release APK (${TAG})"
./gradlew assembleRelease \
  -PversionName="nightly_${DATE}" \
  --quiet

[[ -f "$APK_SRC" ]] || { echo "ERROR: APK not found at $APK_SRC" >&2; exit 1; }
cp "$APK_SRC" "$APK_OUT"
echo "==> APK ready: ${APK_OUT} ($(du -h "$APK_OUT" | cut -f1))"

echo "==> Publishing GitHub release ${TAG}"
gh release delete "$TAG" --yes 2>/dev/null || true
gh release create "$TAG" "$APK_OUT" \
  --repo "$REPO" \
  --title "CarLyrics nightly ${DATE}" \
  --notes "Nightly build ${DATE}

What is in this build:
- LRCLIB synced lyrics (timestamped, karaoke style)
- Android Auto and Android Automotive support
- Home screen widget with live lyric
- Lyrics Library: browse cached lyrics, import LRC files
- Companion View: full screen synced lyrics with auto scroll
- Live notification: shows current lyric line

Install: download ${APK_OUT} and sideload (Settings, Install unknown apps).

Minimum: Android 11 (API 30)" \
  --prerelease

echo "==> Done: https://github.com/${REPO}/releases/tag/${TAG}"
rm -f "$APK_OUT"
