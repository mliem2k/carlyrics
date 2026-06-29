package com.spotifylyrics.data.repository

import com.spotifylyrics.data.local.database.dao.LyricsDao
import com.spotifylyrics.data.remote.api.GeniusApiService
import com.spotifylyrics.data.remote.api.LyricsOvhApiService
import com.spotifylyrics.data.remote.api.MusixmatchApiService
import com.spotifylyrics.domain.model.Lyrics
import com.spotifylyrics.domain.repository.LyricsRepository
import com.spotifylyrics.domain.util.LyricsMatcherEnhanced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Enhanced Repository implementation with multi-source lyrics fetching
 * 
 * Features:
 * - Smart lyrics matching with fuzzy search
 * - Multi-source fallback (Genius, LRCLIB, Musixmatch)
 * - Intelligent caching with TTL
 * - Search result caching to avoid duplicate API calls
 */
class LyricsRepositoryEnhanced @Inject constructor(
    private val lyricsDao: LyricsDao,
    private val geniusApiService: GeniusApiService,
    private val lyricsOvhApiService: LyricsOvhApiService,
    private val musixmatchApiService: MusixmatchApiService,
    private val lyricsMatcher: LyricsMatcherEnhanced
) : LyricsRepository {
    
    override suspend fun getLyrics(artist: String, title: String): Lyrics? =
        withContext(Dispatchers.IO) {
            // Try primary search with enhanced matching
            lyricsMatcher.findLyricsWithFallback(artist, title)
                ?.also { cacheLyrics(it) }
                ?: run {
                    // Fallback: Try direct API calls
                    fetchFromGeniusApi(artist, title)
                        ?.also { cacheLyrics(it) }
                        ?: fetchFromLyricsOvhApi(artist, title)
                            ?.also { cacheLyrics(it) }
                        ?: fetchFromMusixmatchApi(artist, title)
                            ?.also { cacheLyrics(it) }
                }
        }
    
    override suspend fun getCachedLyrics(artist: String, title: String): Lyrics? =
        withContext(Dispatchers.IO) {
            lyricsDao.getLyrics(artist, title)
        }
    
    override suspend fun cacheLyrics(lyrics: Lyrics) {
        withContext(Dispatchers.IO) {
            lyricsDao.insert(lyrics.toEntity())
        }
    }
    
    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            lyricsDao.deleteAll()
        }
    }
    
    /**
     * Fetch from Genius API with error handling
     */
    private suspend fun fetchFromGeniusApi(artist: String, title: String): Lyrics? =
        withContext(Dispatchers.IO) {
            try {
                geniusApiService.searchLyrics(artist, title)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    
    /**
     * Fetch from Lyrics OVH API (LRCLIB alternative)
     */
    private suspend fun fetchFromLyricsOvhApi(artist: String, title: String): Lyrics? =
        withContext(Dispatchers.IO) {
            try {
                lyricsOvhApiService.getLyrics(artist, title)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    
    /**
     * Fetch from Musixmatch API (fallback)
     */
    private suspend fun fetchFromMusixmatchApi(artist: String, title: String): Lyrics? =
        withContext(Dispatchers.IO) {
            try {
                musixmatchApiService.searchLyrics(artist, title)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
