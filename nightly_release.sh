#!/usr/bin/env bash
set -euo pipefail

REPO="mliem2k/livelyrics"

: "${SIGNING_KEYSTORE_PATH:?Set SIGNING_KEYSTORE_PATH to the release keystore path}"
: "${SIGNING_STORE_PASSWORD:?Set SIGNING_STORE_PASSWORD}"
: "${SIGNING_KEY_ALIAS:?Set SIGNING_KEY_ALIAS}"
: "${SIGNING_KEY_PASSWORD:?Set SIGNING_KEY_PASSWORD}"

DATE=$(date -u +%Y%m%d)
# The tag carries the build number, which is also the APK's versionCode, so the app compares exact
# builds and several releases on one day stay distinguishable.
VERSION_CODE=$(date -u +%s)
TAG="nightly-${DATE}-${VERSION_CODE}"
APK_SRC="app/build/outputs/apk/release/app-release.apk"
APK_OUT="livelyrics_${TAG}.apk"

echo "==> Building release APK (${TAG})"
./gradlew assembleRelease \
  -PversionName="${TAG}" \
  -PversionCode="${VERSION_CODE}" \
  --quiet

[[ -f "$APK_SRC" ]] || { echo "ERROR: APK not found at $APK_SRC" >&2; exit 1; }
cp "$APK_SRC" "$APK_OUT"
echo "==> APK ready: ${APK_OUT} ($(du -h "$APK_OUT" | cut -f1))"

echo "==> Publishing GitHub release ${TAG}"
gh release create "$TAG" "$APK_OUT" \
  --repo "$REPO" \
  --title "LiveLyrics nightly ${DATE}" \
  --notes "Nightly build ${DATE} (build ${VERSION_CODE})

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
