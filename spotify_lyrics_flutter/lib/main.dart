import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:provider/provider.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';
import 'lyrics_service.dart';
import 'music_detection_service.dart';

void main() {
  runApp(const SpotifyLyricsApp());
}

class LyricsData extends ChangeNotifier {
  static const MethodChannel _commChannel = MethodChannel('spotify_lyrics/communication');
  static const MethodChannel _musicChannel = MethodChannel('music_detection');
  static const EventChannel _eventChannel = EventChannel('spotify_lyrics/events');

  String _currentTrack = 'No track playing';
  String _currentArtist = '';
  String _currentLyrics = 'Lyrics will appear here when music is detected';
  bool _isConnected = false;
  bool _isLoading = false;
  String _lastError = '';
  bool _isDetectingMusic = false;
  bool _hasNotificationPermission = false;
  bool _isNotificationListenerEnabled = false;
  MusicDetectionService? _musicDetection;
  StreamSubscription? _eventSubscription;

  String get currentTrack => _currentTrack;
  String get currentArtist => _currentArtist;
  String get currentLyrics => _currentLyrics;
  bool get isConnected => _isConnected;
  bool get isLoading => _isLoading;
  String get lastError => _lastError;
  bool get isDetectingMusic => _isDetectingMusic;
  bool get hasNotificationPermission => _hasNotificationPermission;
  bool get isNotificationListenerEnabled => _isNotificationListenerEnabled;

  void initializeMusicDetection() {
    if (_musicDetection == null) {
      _musicDetection = MusicDetectionService();
      _musicDetection!.initialize(
        updateTrack,
        onError: (error) {
          _lastError = error;
          notifyListeners();
        },
      );
      _isDetectingMusic = true;
      notifyListeners();
    }
  }

  void updateTrack(String track, String artist) {
    // Normalize inputs
    final normalizedTrack = track.trim();
    final normalizedArtist = artist.trim();

    // Check if actually changed
    if (normalizedTrack == _currentTrack && normalizedArtist == _currentArtist) {
      return;
    }

    print('Track changed: "$normalizedTrack" by "$normalizedArtist"');
    _currentTrack = normalizedTrack;
    _currentArtist = normalizedArtist;
    _lastError = '';

    if (normalizedTrack.isNotEmpty && normalizedArtist.isNotEmpty) {
      if (normalizedTrack != 'No track playing') {
        _fetchLyrics();
      } else {
        _currentLyrics = 'Waiting for music to play...\n\nPlay some music in Spotify to see lyrics here.';
      }
    } else {
      _currentLyrics = 'Waiting for music to play...\n\nPlay some music in any app (Spotify, Apple Music, YouTube Music, etc.) to see lyrics here.';
    }

    notifyListeners();
  }

  Future<void> checkPermissionStatus() async {
    try {
      final result = await _commChannel.invokeMethod('checkPermissionStatus');
      _isNotificationListenerEnabled = result as bool? ?? false;
      notifyListeners();
    } catch (e) {
      print('Error checking permission: $e');
    }
  }

  Future<void> requestNotificationPermission() async {
    try {
      await _commChannel.invokeMethod('requestNotificationPermission');
      await checkPermissionStatus();
    } catch (e) {
      print('Error requesting permission: $e');
      _lastError = 'Failed to request permission: $e';
      notifyListeners();
    }
  }

  Future<void> openNotificationSettings() async {
    try {
      await _commChannel.invokeMethod('openNotificationSettings');
    } catch (e) {
      print('Error opening settings: $e');
    }
  }

  Future<void> _fetchLyrics() async {
    if (_currentTrack.isEmpty || _currentTrack == 'No track playing') {
      _currentLyrics = 'Lyrics will appear here when music is detected';
      notifyListeners();
      return;
    }

    _isLoading = true;
    _currentLyrics = 'Loading lyrics...';
    notifyListeners();

    try {
      final lyrics = await LyricsService.fetchLyrics(_currentArtist, _currentTrack);

      if (lyrics.contains('not found') || lyrics.isEmpty) {
        _currentLyrics = 'Lyrics not found for this track.\n\nYou can add lyrics manually by long-pressing on this area.';
        _lastError = 'Lyrics not found';
      } else {
        _currentLyrics = lyrics;
        _lastError = '';
      }
    } catch (e) {
      _currentLyrics = 'Error loading lyrics: ${e.toString()}\n\nPlease check your internet connection.';
      _lastError = e.toString();
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<void> retryFetchLyrics() async {
    await _fetchLyrics();
  }

  Future<void> saveLocalLyrics(String lyrics) async {
    if (_currentTrack.isNotEmpty && _currentArtist.isNotEmpty) {
      await LyricsService.saveLocalLyrics(_currentArtist, _currentTrack, lyrics);
      _currentLyrics = lyrics;
      notifyListeners();
    }
  }

  Future<void> clearCache() async {
    final prefs = await SharedPreferences.getInstance();
    final keys = prefs.getKeys().where((k) => k.startsWith('lyrics_cache_')).toList();
    for (var key in keys) {
      await prefs.remove(key);
    }
  }

  void enableMockMode() async {
    try {
      await _commChannel.invokeMethod('enableMockMode');
      _currentLyrics = 'Mock mode enabled - Simulating track changes every 10 seconds';
      notifyListeners();
    } catch (e) {
      _currentLyrics = 'Failed to enable mock mode';
      notifyListeners();
    }
  }

  void disableMockMode() async {
    try {
      await _commChannel.invokeMethod('disableMockMode');
      notifyListeners();
    } catch (e) {
      // Ignore error
    }
  }

  void dispose() {
    _musicDetection?.dispose();
    _eventSubscription?.cancel();
    super.dispose();
  }
}

class SpotifyLyricsApp extends StatelessWidget {
  const SpotifyLyricsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Spotify Lyrics',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF1DB954),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFF121212),
      ),
      home: ChangeNotifierProvider(
        create: (context) => LyricsData(),
        child: const MainScreen(),
      ),
    );
  }
}

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  static const MethodChannel _commChannel = MethodChannel('spotify_lyrics/communication');
  static const MethodChannel _musicChannel = MethodChannel('music_detection');

  StreamSubscription? _musicSubscription;

  @override
  void initState() {
    super.initState();
    _initializeConnection();
    Provider.of<LyricsData>(context, listen: false).initializeMusicDetection();
    _setupEventStream();
  }

  @override
  void dispose() {
    _musicSubscription?.cancel();
    super.dispose();
  }

  Future<void> _initializeConnection() async {
    try {
      // Set up method call handlers first
      _commChannel.setMethodCallHandler(_handleCommMethodCall);
      _musicChannel.setMethodCallHandler(_handleMusicMethodCall);

      // Initialize native side
      final result = await _commChannel.invokeMethod('initialize');

      // Update provider with device info
      if (result is Map) {
        final lyricsData = Provider.of<LyricsData>(context, listen: false);
        lyricsData._isNotificationListenerEnabled = result['isNotificationListenerEnabled'] ?? false;
        lyricsData.notifyListeners();
      }

      // Connection status is now tracked via notification listener enabled state
    } catch (e) {
      print('Failed to initialize: $e');
    }
  }

  void _setupEventStream() {
    // Listen to event channel for continuous updates
    _musicSubscription = const EventChannel('spotify_lyrics/events')
        .receiveBroadcastStream()
        .listen((event) {
      if (event is Map) {
        final track = event['track']?.toString() ?? '';
        final artist = event['artist']?.toString() ?? '';
        if (track.isNotEmpty && artist.isNotEmpty) {
          Provider.of<LyricsData>(context, listen: false).updateTrack(track, artist);
        }
      }
    }, onError: (error) {
      print('Event stream error: $error');
    });
  }

  Future<dynamic> _handleCommMethodCall(MethodCall call) async {
    print('Comm method called: ${call.method}');
    switch (call.method) {
      case 'onTrackChanged':
        if (call.arguments is Map) {
          final Map<String, dynamic> trackInfo = Map<String, dynamic>.from(call.arguments);
          final track = trackInfo['track']?.toString() ?? 'Unknown Track';
          final artist = trackInfo['artist']?.toString() ?? 'Unknown Artist';

          Provider.of<LyricsData>(context, listen: false).updateTrack(track, artist);
        }
        break;
    }
  }

  Future<dynamic> _handleMusicMethodCall(MethodCall call) async {
    print('Music method called: ${call.method}');
    switch (call.method) {
      case 'onTrackChanged':
        if (call.arguments is Map) {
          final Map<String, dynamic> trackInfo = Map<String, dynamic>.from(call.arguments);
          final track = trackInfo['track']?.toString() ?? 'Unknown Track';
          final artist = trackInfo['artist']?.toString() ?? 'Unknown Artist';

          Provider.of<LyricsData>(context, listen: false).updateTrack(track, artist);
        }
        break;
      case 'onMediaSessionChanged':
        if (call.arguments is Map) {
          final Map<String, dynamic> trackInfo = Map<String, dynamic>.from(call.arguments);
          final track = trackInfo['track']?.toString() ?? '';
          final artist = trackInfo['artist']?.toString() ?? '';
          final isPlaying = trackInfo['isPlaying'] ?? false;

          if (isPlaying && track.isNotEmpty && artist.isNotEmpty) {
            Provider.of<LyricsData>(context, listen: false).updateTrack(track, artist);
          }
        }
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<LyricsData>(
      builder: (context, lyricsData, child) {
        return Scaffold(
          appBar: AppBar(
            backgroundColor: const Color(0xFF1DB954),
            title: const Text('Spotify Lyrics'),
            actions: [
              if (lyricsData.lastError.isNotEmpty)
                IconButton(
                  onPressed: () => lyricsData.retryFetchLyrics(),
                  icon: const Icon(Icons.refresh),
                  color: Colors.white,
                  tooltip: 'Retry loading lyrics',
                ),
              PopupMenuButton<String>(
                onSelected: (value) {
                  if (value == 'clear_cache') {
                    lyricsData.clearCache();
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Cache cleared')),
                    );
                  } else if (value == 'enable_mock') {
                    lyricsData.enableMockMode();
                  } else if (value == 'disable_mock') {
                    lyricsData.disableMockMode();
                  } else if (value == 'check_permission') {
                    lyricsData.checkPermissionStatus();
                    _showPermissionStatus(lyricsData);
                  } else if (value == 'request_permission') {
                    lyricsData.requestNotificationPermission();
                    _showPermissionDialog();
                  } else if (value == 'open_settings') {
                    lyricsData.openNotificationSettings();
                  }
                },
                itemBuilder: (context) => [
                  const PopupMenuItem(
                    value: 'check_permission',
                    child: Row(
                      children: [
                        Icon(Icons.security, size: 20),
                        SizedBox(width: 8),
                        Text('Check Permission'),
                      ],
                    ),
                  ),
                  const PopupMenuItem(
                    value: 'request_permission',
                    child: Row(
                      children: [
                        Icon(Icons.notification_important, size: 20),
                        SizedBox(width: 8),
                        Text('Enable Notification Access'),
                      ],
                    ),
                  ),
                  const PopupMenuItem(
                    value: 'open_settings',
                    child: Row(
                      children: [
                        Icon(Icons.settings, size: 20),
                        SizedBox(width: 8),
                        Text('Open Settings'),
                      ],
                    ),
                  ),
                  const PopupMenuItem(
                    value: 'clear_cache',
                    child: Row(
                      children: [
                        Icon(Icons.clear_all, size: 20),
                        SizedBox(width: 8),
                        Text('Clear Cache'),
                      ],
                    ),
                  ),
                  const PopupMenuItem(
                    value: 'enable_mock',
                    child: Row(
                      children: [
                        Icon(Icons.music_note, size: 20),
                        SizedBox(width: 8),
                        Text('Test Mode'),
                      ],
                    ),
                  ),
                  const PopupMenuItem(
                    value: 'disable_mock',
                    child: Row(
                      children: [
                        Icon(Icons.music_off, size: 20),
                        SizedBox(width: 8),
                        Text('Stop Test Mode'),
                      ],
                    ),
                  ),
                ],
                child: const Icon(Icons.more_vert),
                color: Colors.white,
              ),
              const SizedBox(width: 8),
              Icon(
                lyricsData.isNotificationListenerEnabled ? Icons.notifications_active : Icons.notifications_none,
                color: Colors.white,
              ),
              const SizedBox(width: 16),
            ],
          ),
          body: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (!lyricsData.isNotificationListenerEnabled)
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFA726),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.warning, color: Colors.white),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Notification Access Required',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const Text(
                                'Enable notification access to detect playing music',
                                style: TextStyle(color: Colors.white70, fontSize: 12),
                              ),
                            ],
                          ),
                        ),
                        TextButton(
                          onPressed: () => lyricsData.requestNotificationPermission(),
                          child: const Text('ENABLE', style: TextStyle(color: Colors.white)),
                        ),
                      ],
                    ),
                  ),
                if (!lyricsData.isNotificationListenerEnabled) const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFF282828),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Now Playing:',
                        style: TextStyle(
                          color: Colors.grey[400],
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        lyricsData.currentTrack,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        lyricsData.currentArtist,
                        style: TextStyle(
                          color: Colors.grey[300],
                          fontSize: 16,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                Expanded(
                  child: GestureDetector(
                    onLongPress: lyricsData.lastError.isNotEmpty
                        ? () {
                            _showAddLyricsDialog(context, lyricsData);
                          }
                        : null,
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: const Color(0xFF282828),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Stack(
                        children: [
                          if (lyricsData.isLoading)
                            const Center(
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  CircularProgressIndicator(
                                    valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF1DB954)),
                                  ),
                                  SizedBox(height: 16),
                                  Text(
                                    'Searching for lyrics...',
                                    style: TextStyle(
                                      color: Colors.grey,
                                      fontSize: 16,
                                    ),
                                  ),
                                ],
                              ),
                            )
                          else
                            SingleChildScrollView(
                              child: Column(
                                children: [
                                  Text(
                                    lyricsData.currentLyrics,
                                    style: TextStyle(
                                      color: lyricsData.lastError.isNotEmpty
                                          ? Colors.grey[400]
                                          : Colors.white,
                                      fontSize: 16,
                                      height: 1.5,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),
                                  if (lyricsData.lastError.isNotEmpty) ...[
                                    const SizedBox(height: 20),
                                    Text(
                                      'Tip: Long-press to add lyrics manually',
                                      style: const TextStyle(
                                        color: Color(0xFF1DB954),
                                        fontSize: 14,
                                        fontStyle: FontStyle.italic,
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _showPermissionDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Enable Notification Access'),
        content: const Text(
          'To detect playing music, this app needs notification access.\n\n'
          '1. Open the notification access settings\n'
          '2. Find "Spotify Lyrics"\n'
          '3. Enable the toggle\n'
          '4. Come back to this app',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              Provider.of<LyricsData>(context, listen: false).openNotificationSettings();
            },
            child: const Text('Open Settings'),
          ),
        ],
      ),
    );
  }

  void _showPermissionStatus(LyricsData lyricsData) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
            'Notification Access: ${lyricsData.isNotificationListenerEnabled ? "Granted" : "Not Granted"}'),
        duration: const Duration(seconds: 3),
      ),
    );
  }

  void _showAddLyricsDialog(BuildContext context, LyricsData lyricsData) {
    final controller = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          'Add Lyrics for ${lyricsData.currentTrack}',
          style: const TextStyle(color: Color(0xFF1DB954)),
        ),
        content: TextField(
          controller: controller,
          maxLines: 10,
          autofocus: true,
          decoration: const InputDecoration(
            hintText: 'Enter lyrics here...',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                lyricsData.saveLocalLyrics(controller.text);
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Lyrics saved successfully!'),
                    backgroundColor: Color(0xFF1DB954),
                  ),
                );
              }
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF1DB954),
              foregroundColor: Colors.white,
            ),
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }
}
