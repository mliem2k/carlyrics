#!/bin/bash

# Build and Sign Script for Spotify Lyrics App
# Creates both debug and release APKs

echo "🎵 Building Spotify Lyrics for Android Auto..."
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Android SDK is installed
if [ -z "$ANDROID_HOME" ]; then
    if command -v adb &> /dev/null; then
        export ANDROID_HOME=$(dirname $(dirname $(which adb)))
    else
        print_error "ANDROID_HOME not set and adb not found"
        print_error "Please install Android SDK or set ANDROID_HOME"
        exit 1
    fi
fi

print_status "Using Android SDK at: $ANDROID_HOME"

# Create output directory
OUTPUT_DIR="build/outputs/apk"
mkdir -p $OUTPUT_DIR

# Build variants
BUILD_VARIANTS=("debug" "release")

for variant in "${BUILD_VARIANTS[@]}"; do
    print_status "Building $variant variant..."

    if [ "$variant" == "release" ]; then
        # For release build, we need to check for keystore
        if [ ! -f "app/release.keystore" ]; then
            print_warning "Release keystore not found. Creating one..."
            print_warning "This is a TEST keystore - DO NOT use in production!"

            # Create a simple test keystore
            keytool -genkeypair \
                -v \
                -storetype PKCS12 \
                -keystore app/release.keystore \
                -storepass release123 \
                -alias spotifylyrics \
                -keypass release123 \
                -keyalg RSA \
                -keysize 2048 \
                -validity 10000 \
                -dname "CN=Spotify Lyrics, OU=Test, O=Test, L=Test, S=Test, C=US" \
                2>/dev/null || print_warning "Could not create release keystore automatically"
        fi
    fi

    # Build the APK
    if ./gradlew "assemble$variant"; then
        print_status "✅ $variant build successful!"

        # Copy APK to output directory
        APK_PATH="app/build/outputs/apk/$variant/app-$variant.apk"
        if [ -f "$APK_PATH" ]; then
            cp "$APK_PATH" "$OUTPUT_DIR/SpotifyLyrics-$variant.apk"
            print_status "📦 APK copied to: $OUTPUT_DIR/SpotifyLyrics-$variant.apk"
        else
            print_error "APK not found at expected location: $APK_PATH"
        fi
    else
        print_error "❌ $variant build failed!"
    fi

    echo ""
done

# Get APK info
print_status "Build Summary:"
echo "============================"

for variant in "${BUILD_VARIANTS[@]}"; do
    APK_FILE="$OUTPUT_DIR/SpotifyLyrics-$variant.apk"
    if [ -f "$APK_FILE" ]; then
        SIZE=$(du -h "$APK_FILE" | cut -f1)
        print_status "$variant APK: $SIZE"
    fi
done

# Instructions for installation
echo ""
print_status "📋 Installation Instructions:"
echo "============================"
echo ""
echo "1. Debug APK (for testing):"
echo "   adb install -r build/outputs/apk/SpotifyLyrics-debug.apk"
echo ""
echo "2. Release APK (for distribution):"
echo "   adb install -r build/outputs/apk/SpotifyLyrics-release.apk"
echo ""
echo "3. Or transfer APK to device and install manually"
echo ""
echo "4. For Android Auto testing:"
echo "   - Connect device to car"
echo "   - Or use Android Auto Desktop Head Unit"
echo ""
print_status "✅ Build complete! 🎉"
echo ""
print_warning "Note: The release keystore is for testing only"
print_warning "For production, generate your own keystore!"