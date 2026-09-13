import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:spotify_lyrics_flutter/main.dart';

void main() {
  testWidgets('Spotify Lyrics app smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const SpotifyLyricsApp());

    // Verify that the app loads with correct title
    expect(find.text('Spotify Lyrics'), findsOneWidget);

    // Verify initial state shows "No track playing"
    expect(find.text('No track playing'), findsOneWidget);

    // Verify lyrics placeholder text
    expect(find.text('Lyrics will appear here when a track is playing'), findsOneWidget);
  });
}
