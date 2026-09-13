import 'package:flutter/services.dart';
import 'dart:async';

class MusicDetectionService {
  static const MethodChannel _channel = MethodChannel('music_detection');
  static const MethodChannel _spotifyChannel = MethodChannel('spotify_detection');
  static const EventChannel _eventChannel = EventChannel('spotify_lyrics/events');

  StreamSubscription? _mediaSubscription;
  StreamSubscription? _eventSubscription;
  Function(String track, String artist)? _onTrackChanged;
  Function(String error)? _onError;
  Timer? _pollingTimer;
  String _lastTrack = '';
  String _lastArtist = '';

  /// Initialize the music detection service
  Future<void> initialize(
    Function(String track, String artist) onTrackChanged, {
    Function(String error)? onError,
  }) async {
    _onTrackChanged = onTrackChanged;
    _onError = onError;

    try {
      // Set up method call handler for native callbacks
      _channel.setMethodCallHandler(_handleMusicMethodCall);

      // Listen to event channel for continuous updates
      _eventSubscription = _eventChannel.receiveBroadcastStream().listen(
        (event) {
          if (event is Map) {
            final track = event['track']?.toString() ?? '';
            final artist = event['artist']?.toString() ?? '';
            final isPlaying = event['isPlaying'] ?? false;

            if (isPlaying && track.isNotEmpty && artist.isNotEmpty) {
              _notifyTrackChanged(track, artist);
            }
          }
        },
        onError: (error) {
          _onError?.call('Event stream error: $error');
        },
      );

      // Try to initialize Spotify SDK first
      await _initializeSpotifyDetection();

      // Start polling as a fallback
      _startPolling();
    } catch (e) {
      _onError?.call('Failed to initialize music detection: $e');
      // Start polling as fallback
      _startPolling();
    }
  }

  /// Try to detect Spotify via Spotify SDK
  Future<void> _initializeSpotifyDetection() async {
    try {
      // Check if Spotify app is installed
      final bool isSpotifyInstalled = await _channel.invokeMethod('isSpotifyInstalled');

      if (isSpotifyInstalled) {
        // Try to connect to Spotify
        _spotifyChannel.setMethodCallHandler(_handleSpotifyMethodCall);
        await _spotifyChannel.invokeMethod('initialize');
      }
    } catch (e) {
      _onError?.call('Spotify detection not available: $e');
    }
  }

  /// Start polling for currently playing track as fallback
  void _startPolling() {
    _pollingTimer = Timer.periodic(const Duration(seconds: 2), (timer) async {
      await _checkCurrentlyPlaying();
    });
  }

  /// Check currently playing track via notifications or accessibility
  Future<void> _checkCurrentlyPlaying() async {
    try {
      final Map<String, dynamic>? trackInfo = await _channel.invokeMapMethod('getCurrentlyPlaying');

      if (trackInfo != null) {
        final track = trackInfo['track']?.toString() ?? '';
        final artist = trackInfo['artist']?.toString() ?? '';
        final isPlaying = trackInfo['isPlaying'] ?? false;

        if (isPlaying && (track != _lastTrack || artist != _lastArtist)) {
          _lastTrack = track;
          _lastArtist = artist;
          _notifyTrackChanged(track, artist);
        }
      }
    } catch (e) {
      // Polling failed, continue trying
    }
  }

  void _notifyTrackChanged(String track, String artist) {
    if (track != _lastTrack || artist != _lastArtist) {
      _lastTrack = track;
      _lastArtist = artist;
      _onTrackChanged?.call(track, artist);
    }
  }

  /// Handle method calls from Spotify SDK
  Future<dynamic> _handleSpotifyMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onTrackChanged':
        if (call.arguments is Map) {
          final Map<String, dynamic> trackInfo = Map<String, dynamic>.from(call.arguments);
          final track = trackInfo['track'] ?? '';
          final artist = trackInfo['artist'] ?? '';

          if (track.isNotEmpty && artist.isNotEmpty) {
            _notifyTrackChanged(track, artist);
          }
        }
        break;

      case 'onPlaybackStateChanged':
        // Handle play/pause if needed
        break;
    }
  }

  /// Handle method calls from Music Detection
  Future<dynamic> _handleMusicMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onTrackChanged':
        if (call.arguments is Map) {
          final Map<String, dynamic> trackInfo = Map<String, dynamic>.from(call.arguments);
          final track = trackInfo['track'] ?? '';
          final artist = trackInfo['artist'] ?? '';

          if (track.isNotEmpty && artist.isNotEmpty) {
            _notifyTrackChanged(track, artist);
          }
        }
        break;

      case 'onMediaSessionChanged':
        if (call.arguments is Map) {
          final Map<String, dynamic> trackInfo = Map<String, dynamic>.from(call.arguments);
          final track = trackInfo['track'] ?? '';
          final artist = trackInfo['artist'] ?? '';
          final isPlaying = trackInfo['isPlaying'] ?? false;

          if (isPlaying && track.isNotEmpty && artist.isNotEmpty) {
            _notifyTrackChanged(track, artist);
          }
        }
        break;
    }
  }

  /// Get currently playing track manually
  Future<Map<String, String>?> getCurrentlyPlayingTrack() async {
    try {
      final Map<String, dynamic>? trackInfo = await _channel.invokeMapMethod('getCurrentlyPlaying');

      if (trackInfo != null) {
        return {
          'track': trackInfo['track']?.toString() ?? '',
          'artist': trackInfo['artist']?.toString() ?? '',
          'album': trackInfo['album']?.toString() ?? '',
          'duration': trackInfo['duration']?.toString() ?? '',
        };
      }
    } catch (e) {
      _onError?.call('Failed to get current track: $e');
    }
    return null;
  }

  /// Check if any music is currently playing
  Future<bool> isMusicPlaying() async {
    try {
      final Map<String, dynamic>? trackInfo = await _channel.invokeMapMethod('getCurrentlyPlaying');
      return trackInfo?['isPlaying'] ?? false;
    } catch (e) {
      return false;
    }
  }

  /// Get list of installed music apps
  Future<List<String>> getInstalledMusicApps() async {
    try {
      final List<dynamic>? apps = await _channel.invokeListMethod('getInstalledMusicApps');
      return apps?.map((e) => e.toString()).toList() ?? [];
    } catch (e) {
      return [];
    }
  }

  /// Dispose of all resources
  void dispose() {
    _pollingTimer?.cancel();
    _mediaSubscription?.cancel();
    _eventSubscription?.cancel();
    _channel.invokeMethod('stopDetection');
  }
}

/// Mock implementation for testing/development
class MockMusicDetectionService {
  static final List<Map<String, String>> _mockTracks = [
    {'track': 'Bohemian Rhapsody', 'artist': 'Queen'},
    {'track': 'Imagine', 'artist': 'John Lennon'},
    {'track': 'Hotel California', 'artist': 'Eagles'},
    {'track': 'Stairway to Heaven', 'artist': 'Led Zeppelin'},
    {'track': 'Sweet Child O Mine', 'artist': "Guns N' Roses"},
  ];

  static Timer? _mockTimer;
  static int _currentIndex = 0;

  static void startMockDetection(Function(String track, String artist) onTrackChanged) {
    _mockTimer?.cancel();
    _mockTimer = Timer.periodic(const Duration(seconds: 10), (timer) {
      final track = _mockTracks[_currentIndex];
      onTrackChanged(track['track']!, track['artist']!);
      _currentIndex = (_currentIndex + 1) % _mockTracks.length;
    });

    // Trigger first track immediately
    final track = _mockTracks[_currentIndex];
    onTrackChanged(track['track']!, track['artist']!);
  }

  static void stopMockDetection() {
    _mockTimer?.cancel();
  }
}
