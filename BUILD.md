# Build Instructions - Spotify Lyrics for Android Auto

## Quick Start (Easiest)

```bash
# 1. Clone and navigate to project
cd android-auto-lyrics

# 2. Build debug APK
./quick-build.sh

# 3. Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or use the installer
./install.sh
```

## Requirements

- Android Studio OR Android SDK command-line tools
- Java 17 (usually comes with Android Studio)
- Android device with USB debugging enabled

## Build Options

### Option 1: Quick Build (Debug)
```bash
./quick-build.sh
```
- Builds only debug APK
- Uses default debug keystore
- Fastest option

### Option 2: Full Build (Debug + Release)
```bash
./build-and-sign.sh
```
- Builds both debug and release
- Creates test keystore for release
- Optimized release APK

### Option 3: Manual Gradle
```bash
# Debug
./gradlew assembleDebug

# Release (requires keystore setup)
./gradlew assembleRelease
```

## APK Locations

After building, APKs are in:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Signing

### Debug APK
- Uses Android's default debug keystore
- Not suitable for Play Store
- Perfect for testing

### Release APK
- The build script creates a test keystore
- For production: generate your own keystore
  ```bash
  keytool -genkey -v -keystore my-release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000
  ```

## Install on Device

### Method 1: ADB (Recommended)
```bash
adb devices  # Check device is connected
adb install -r app-debug.apk
```

### Method 2: Use Installer Script
```bash
./install.sh
```

### Method 3: Manual
1. Transfer APK to device
2. Enable "Install from unknown sources"
3. Open APK file to install

## Troubleshooting

### "command not found: adb"
- Add Android SDK to PATH
- Or use full path: `$ANDROID_HOME/platform-tools/adb`

### "Unable to locate a Java Runtime"
- Install JDK 17
- Or use Android Studio's bundled JDK

### Build fails
- Run `./gradlew clean` first
- Check Android SDK installation
- Update to latest Gradle wrapper: `./gradlew wrapper --gradle-version=8.7.3`

## Android Auto Testing

1. Install the app on your phone
2. Connect to Android Auto (USB or wireless)
3. Look for "Spotify Lyrics" in the car launcher

## Verification

After installation:
1. Open Spotify and play any song
2. Open "Spotify Lyrics" app
3. Lyrics should appear synced with music
4. In Android Auto, lyrics should display on car screen