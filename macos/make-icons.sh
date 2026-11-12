#!/usr/bin/env bash
# Regenerates the committed icons in Resources/ (needs rsvg-convert: brew install librsvg).
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RES="$APP_DIR/Resources"
APP_ICON_SVG="$APP_DIR/../landing/public/favicon.svg"

rsvg-convert -w 18 -h 18 "$RES/MenuBarIcon.svg" -o "$RES/MenuBarIcon.png"
rsvg-convert -w 36 -h 36 "$RES/MenuBarIcon.svg" -o "$RES/MenuBarIcon@2x.png"

ICONSET="$(mktemp -d)/AppIcon.iconset"
mkdir -p "$ICONSET"
for size in 16 32 128 256 512; do
  rsvg-convert -w "$size" -h "$size" "$APP_ICON_SVG" -o "$ICONSET/icon_${size}x${size}.png"
  rsvg-convert -w $((size * 2)) -h $((size * 2)) "$APP_ICON_SVG" -o "$ICONSET/icon_${size}x${size}@2x.png"
done
iconutil -c icns "$ICONSET" -o "$RES/AppIcon.icns"
