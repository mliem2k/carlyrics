# Manual Release Build Instructions

Since we're having Java environment issues, here's how to build the release APK:

## Method 1: Use Android Studio (Recommended)

1. **Open Android Studio**
2. **File → Open** → Select the `android-auto-lyrics` folder
3. **Wait for Gradle sync** (may take a few minutes)
4. **Build → Generate Signed Bundle / APK**
5. **Select APK**
6. **Create new keystore** (or use existing):
   - Keystore path: `release.keystore`
   - Password: `release123`
   - Key alias: `spotifylyrics`
   - Key password: `release123`
7. **Select release build variant**
8. **Finish** → APK will be built

## Method 2: Command Line (After Java Setup)

1. **Install JDK 17+**:
   ```bash
   # macOS with Homebrew
   brew install openjdk@17

   # OR download from Oracle
   # https://www.oracle.com/java/technologies/downloads/
   ```

2. **Set JAVA_HOME**:
   ```bash
   export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
   ```

3. **Build**:
   ```bash
   ./gradlew assembleRelease
   ```

## Method 3: Pre-built APK

If you can't build, you can use the debug APK (works the same):
```bash
# If gradlew works
./gradlew assembleDebug

# APK location:
app/build/outputs/apk/debug/app-debug.apk
```

## Files to Check

The project should have these files:
- `app/build.gradle.kts` - Build configuration
- `gradle/wrapper/gradle-wrapper.jar` - Gradle wrapper
- `gradlew` - Gradle script (executable)

## Quick Install Commands

```bash
# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or install release (once built)
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Troubleshooting

### "Unable to locate a Java Runtime"
```bash
# Try different Java locations
export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null)
export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/A

# OR install with Homebrew
brew install --cask temurin17
```

### "gradlew: command not found"
```bash
# Make executable
chmod +x gradlew

# Check if file exists
ls -la gradlew
```

### "SDK not found"
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

## What You Get

Once built, you'll have:
- **SpotifyLyrics-debug.apk** - 5-10MB (for testing)
- **SpotifyLyrics-release.apk** - 3-5MB (for distribution)

Both APKs work identically, release is just smaller and optimized.