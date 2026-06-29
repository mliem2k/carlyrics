#!/bin/bash

echo "🎵 Creating Production Keystore"
echo "=============================="

KEYSTORE_NAME="spotify_lyrics_release"
KEY_ALIAS="spotify_lyrics"
STORE_PASSWORD="spotify_lyrics_2024"
KEY_PASSWORD="spotify_lyrics_2024"

# Create keystore directory
mkdir -p android/keystore

echo "🔐 Generating keystore..."
keytool -genkeypair \
  -v \
  -keystore android/keystore/$KEYSTORE_NAME.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias $KEY_ALIAS \
  -dname "CN=Spotify Lyrics, OU=Mobile Development, O=Spotify Lyrics, L=San Francisco, ST=California, C=US" \
  -storepass $STORE_PASSWORD \
  -keypass $KEY_PASSWORD \
  -keyalg RSA \
  -sigalg SHA256withRSA

echo "✅ Keystore created: android/keystore/$KEYSTORE_NAME.jks"

# Create keystore properties file
cat > android/keystore/keystore.properties << EOF
storePassword=$STORE_PASSWORD
keyPassword=$KEY_PASSWORD
keyAlias=$KEY_ALIAS
storeFile=../keystore/$KEYSTORE_NAME.jks
EOF

echo "✅ Keystore properties created: android/keystore/keystore.properties"

echo ""
echo "📋 Keystore Details:"
echo "• File: android/keystore/$KEYSTORE_NAME.jks"
echo "• Alias: $KEY_ALIAS"
echo "• Validity: 10,000 days"
echo "• Algorithm: RSA 2048-bit"
echo "• Signature: SHA256withRSA"
echo ""
echo "⚠️  IMPORTANT: Save these credentials securely!"
echo "• Store Password: $STORE_PASSWORD"
echo "• Key Password: $KEY_PASSWORD"
echo "• Key Alias: $KEY_ALIAS"
echo ""
echo "🔧 Next Steps:"
echo "1. Update build.gradle to use this keystore"
echo "2. Build signed release APK"
echo "3. Test APK installation"