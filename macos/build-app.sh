#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_BUNDLE="$APP_DIR/build/LiveLyrics.app"

# Universal, so the release runs on Intel and Apple silicon Macs.
BUILD_ARGS=(--package-path "$APP_DIR" --configuration release --arch arm64 --arch x86_64)
swift build "${BUILD_ARGS[@]}"
BIN_DIR="$(swift build "${BUILD_ARGS[@]}" --show-bin-path)"

rm -rf "$APP_BUNDLE"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
cp "$BIN_DIR/LiveLyrics" "$APP_BUNDLE/Contents/MacOS/LiveLyrics"
cp "$APP_DIR/Info.plist" "$APP_BUNDLE/Contents/Info.plist"
mkdir -p "$APP_BUNDLE/Contents/Resources"
cp "$APP_DIR"/Resources/*.png "$APP_DIR"/Resources/*.icns "$APP_BUNDLE/Contents/Resources/"
chmod +x "$APP_BUNDLE/Contents/MacOS/LiveLyrics"
codesign --force --options runtime \
  --entitlements "$APP_DIR/LiveLyrics.entitlements" \
  --sign - "$APP_BUNDLE"

printf 'Built %s\n' "$APP_BUNDLE"
