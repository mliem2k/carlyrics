package com.spotifylyrics.domain.repository

import com.spotifylyrics.domain.model.Lyrics
import com.spotifylyrics.domain.model.TrackInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for lyrics operations
 */
interface LyricsRepository {
    /**
     * Get lyrics for a track, first checking cache then fetching from API
     */
    suspend fun getLyrics(trackInfo: TrackInfo): Result<Lyrics>

    /**
     * Get cached lyrics for a track
     */
    fun getCachedLyrics(track: String, artist: String): Flow<Lyrics?>

    /**
     * Cache lyrics for a track
     */
    suspend fun cacheLyrics(lyrics: Lyrics, source: String)

    /**
     * Clear all cached lyrics
     */
    suspend fun clearCache()

    /**
     * Clear expired cached lyrics
     */
    suspend fun clearExpiredCache()

    /**
     * Get all cached lyrics
     */
    fun getAllCachedLyrics(): Flow<List<Lyrics>>

    /**
     * Search cached lyrics by query
     */
    fun searchLyrics(query: String): Flow<List<Lyrics>>

    /**
     * Import lyrics from LRC file content
     */
    suspend fun importLrcFile(track: String, artist: String, lrcContent: String): Result<Lyrics>

    /**
     * Export lyrics to LRC format
     */
    suspend fun exportToLrc(lyrics: Lyrics): Result<String>
}
