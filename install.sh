#!/bin/bash

# Install script for Spotify Lyrics App
# Automatically detects connected devices and installs

echo "📱 Installing Spotify Lyrics App..."
echo "=================================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check for connected devices
DEVICES=$(adb devices | grep -v "List of devices" | grep -v "offline" | wc -l)

if [ $DEVICES -eq 0 ]; then
    print_error "No Android devices connected!"
    echo ""
    echo "Please:"
    echo "1. Enable Developer Options on your device"
    echo "2. Enable USB Debugging"
    echo "3. Connect via USB"
    echo ""
    exit 1
fi

print_status "Found $DEVICES device(s) connected"

# List connected devices
echo ""
print_status "Connected devices:"
adb devices | grep -v "List of devices" | grep -v "offline"

# Choose APK variant
echo ""
echo "Which APK would you like to install?"
echo "1) Debug (recommended for testing)"
echo "2) Release (optimized version)"
read -p "Enter choice [1]: " CHOICE

CHOICE=${CHOICE:-1}

if [ "$CHOICE" == "1" ]; then
    APK_PATH="build/outputs/apk/SpotifyLyrics-debug.apk"
    VARIANT="debug"
elif [ "$CHOICE" == "2" ]; then
    APK_PATH="build/outputs/apk/SpotifyLyrics-release.apk"
    VARIANT="release"
else
    print_error "Invalid choice"
    exit 1
fi

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    print_error "APK not found: $APK_PATH"
    echo ""
    print_status "Building APK first..."
    ./build-and-sign.sh
    echo ""
fi

# Install the app
print_status "Installing $VARIANT APK..."

if adb install -r "$APK_PATH"; then
    print_status "✅ Installation successful!"
    echo ""
    print_status "📋 Next Steps:"
    echo "=================="
    echo ""
    echo "1. Open 'Spotify Lyrics' on your phone"
    echo "2. Play music on Spotify"
    echo "3. Watch the lyrics appear! 🎵"
    echo ""
    echo "4. For Android Auto:"
    echo "   - Connect to your car"
    echo "   - Look for 'Spotify Lyrics' in Android Auto"
    echo ""
else
    print_error "❌ Installation failed!"
    print_status "Trying alternative installation..."

    # Try with -d flag (specify device)
    DEVICE_ID=$(adb devices | grep -v "List of devices" | grep -v "offline" | head -1 | cut -f1)
    if adb -s "$DEVICE_ID" install -r "$APK_PATH"; then
        print_status "✅ Installation successful on device $DEVICE_ID!"
    else
        print_error "Installation failed completely"
        print_status "Manual installation:"
        echo "1. Transfer $APK_PATH to your device"
        echo "2. Enable 'Install from unknown sources' in settings"
        echo "3. Open the APK file and install"
    fi
fi

echo ""
print_status "Done! 🎉"