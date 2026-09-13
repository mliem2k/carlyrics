import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class LyricsService {
  static const String _cacheKey = 'lyrics_cache_';
  static const Duration _cacheExpiry = Duration(days: 7);

  static Future<String> fetchLyrics(String artist, String title) async {
    if (artist.isEmpty || title.isEmpty) {
      return 'Artist and title required';
    }

    // Check cache first
    final cachedLyrics = await _getCachedLyrics(artist, title);
    if (cachedLyrics != null) {
      return cachedLyrics;
    }

    // Try different sources
    String lyrics = await _tryLyricsOvh(artist, title);

    if (lyrics.isEmpty || lyrics.contains('not found')) {
      lyrics = await _TryGeniusAPI(artist, title);
    }

    if (lyrics.isEmpty || lyrics.contains('not found')) {
      lyrics = await _tryMusixmatch(artist, title);
    }

    if (lyrics.isEmpty || lyrics.contains('not found')) {
      lyrics = await _tryLocalScrape(artist, title);
    }

    if (lyrics.isEmpty) {
      lyrics = 'Lyrics not found';
    } else {
      // Cache the results
      await _cacheLyrics(artist, title, lyrics);
    }

    return lyrics;
  }

  static Future<String> _getCachedLyrics(String artist, String title) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final key = '$_cacheKey${artist.toLowerCase()}_${title.toLowerCase()}';
      final cached = prefs.getString(key);

      if (cached != null) {
        final data = json.decode(cached);
        final timestamp = DateTime.parse(data['timestamp']);

        if (DateTime.now().difference(timestamp) < _cacheExpiry) {
          return data['lyrics'];
        }
      }
    } catch (e) {
      // Cache read failed, continue with API fetch
    }
    return '';
  }

  static Future<void> _cacheLyrics(String artist, String title, String lyrics) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final key = '$_cacheKey${artist.toLowerCase()}_${title.toLowerCase()}';
      final data = {
        'lyrics': lyrics,
        'timestamp': DateTime.now().toIso8601String(),
      };
      await prefs.setString(key, json.encode(data));
    } catch (e) {
      // Cache write failed, but that's okay
    }
  }

  static Future<String> _tryLyricsOvh(String artist, String title) async {
    try {
      final cleanArtist = artist.replaceAll(RegExp(r'[^\w\s]'), '').trim();
      final cleanTitle = title.replaceAll(RegExp(r'[^\w\s]'), '').trim();

      final url = 'https://api.lyrics.ovh/v1/${Uri.encodeComponent(cleanArtist)}/${Uri.encodeComponent(cleanTitle)}';
      final response = await http.get(
        Uri.parse(url),
        headers: {'User-Agent': 'SpotifyLyrics/1.0'},
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['lyrics'] != null && data['lyrics'].toString().isNotEmpty) {
          return _cleanLyrics(data['lyrics']);
        }
      }
    } catch (e) {
      // API failed, continue
    }
    return '';
  }

  static Future<String> _TryGeniusAPI(String artist, String title) async {
    // Genius search without authentication (limited but free)
    try {
      final query = '$artist $title';
      final searchUrl = 'https://genius.com/api/search/multi?q=${Uri.encodeComponent(query)}';

      final response = await http.get(
        Uri.parse(searchUrl),
        headers: {'User-Agent': 'SpotifyLyrics/1.0'},
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final hits = data['response']['sections'][0]['hits'] ?? [];

        for (var hit in hits) {
          if (hit['type'] == 'song' && hit['result']['primary_artist']['name'].toString().toLowerCase().contains(artist.toLowerCase())) {
            final songUrl = hit['result']['url'];
            return await _scrapeGeniusLyrics(songUrl);
          }
        }
      }
    } catch (e) {
      // API failed
    }
    return '';
  }

  static Future<String> _scrapeGeniusLyrics(String url) async {
    try {
      final response = await http.get(
        Uri.parse(url),
        headers: {'User-Agent': 'Mozilla/5.0'},
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final html = response.body;

        // Simple scraping - look for lyrics container
        final startIdx = html.indexOf('"lyrics":');
        if (startIdx != -1) {
          final lyricsStart = html.indexOf('"', startIdx + 10) + 1;
          final lyricsEnd = html.indexOf('"', lyricsStart);

          if (lyricsEnd > lyricsStart) {
            var lyrics = html.substring(lyricsStart, lyricsEnd);
            lyrics = lyrics.replaceAll(r'\n', '\n').replaceAll(r'\\"', '"');
            return _cleanLyrics(lyrics);
          }
        }
      }
    } catch (e) {
      // Scraping failed
    }
    return '';
  }

  static Future<String> _tryMusixmatch(String artist, String title) async {
    // Note: Musixmatch requires API key for official use
    // This is a simplified approach that might work for some tracks
    try {
      // Using a public endpoint (limited functionality)
      final query = Uri.encodeComponent('$artist $title');
      final url = 'https://www.musixmatch.com/search/$query';

      final response = await http.get(
        Uri.parse(url),
        headers: {'User-Agent': 'Mozilla/5.0'},
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        // This would require complex scraping and might break terms
        // Keeping placeholder for now
      }
    } catch (e) {
      // API failed
    }
    return '';
  }

  static Future<String> _tryLocalScrape(String artist, String title) async {
    // This would integrate with local lyrics files
    // Users could provide their own lyrics files
    try {
      final prefs = await SharedPreferences.getInstance();
      final localKey = 'local_lyrics_${artist.toLowerCase()}_${title.toLowerCase()}';
      final localLyrics = prefs.getString(localKey);

      if (localLyrics != null && localLyrics.isNotEmpty) {
        return localLyrics;
      }
    } catch (e) {
      // Local read failed
    }
    return '';
  }

  static String _cleanLyrics(String lyrics) {
    if (lyrics.isEmpty) return lyrics;

    // Remove common prefixes/suffixes
    var cleaned = lyrics;

    // Remove [Intro], [Chorus], etc. but keep them for structure
    // Instead, format them nicely
    cleaned = cleaned.replaceAllMapped(
      RegExp(r'\[([^\]]+)\]', caseSensitive: false),
      (match) => '\n--- ${match.group(1)?.toUpperCase() ?? ''} ---\n',
    );

    // Clean up extra whitespace
    cleaned = cleaned.replaceAll(RegExp(r'\n\s*\n\s*\n'), '\n\n');
    cleaned = cleaned.trim();

    // Remove any copyright notices at the end
    final lines = cleaned.split('\n');
    final filteredLines = lines.where((line) {
      final lower = line.toLowerCase();
      return !lower.contains('lyrics provided by') &&
             !lower.contains('copyright') &&
             !lower.contains('writers:') &&
             !lower.contains('songwriters:') &&
             !line.contains('---');
    }).toList();

    return filteredLines.join('\n');
  }

  static Future<void> saveLocalLyrics(String artist, String title, String lyrics) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final key = 'local_lyrics_${artist.toLowerCase()}_${title.toLowerCase()}';
      await prefs.setString(key, lyrics);
    } catch (e) {
      // Save failed
    }
  }

  static Future<List<Map<String, String>>> getCachedLyricsList() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final keys = prefs.getKeys().where((k) => k.startsWith(_cacheKey)).toList();

      final List<Map<String, String>> cached = [];
      for (var key in keys) {
        final value = prefs.getString(key);
        if (value != null) {
          final data = json.decode(value);
          final parts = key.replaceFirst(_cacheKey, '').split('_');
          if (parts.length >= 2) {
            cached.add({
              'artist': parts[0],
              'title': parts.sublist(1).join('_'),
              'preview': (data['lyrics'] as String).substring(0, 100) + '...',
            });
          }
        }
      }

      return cached;
    } catch (e) {
      return [];
    }
  }
}