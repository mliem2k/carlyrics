package com.spotifylyrics.data.repository

import com.spotifylyrics.data.local.database.dao.LyricsDao
import com.spotifylyrics.data.local.database.entity.toDomainModel
import com.spotifylyrics.data.local.database.entity.toEntity
import com.spotifylyrics.data.remote.api.GeniusApiService
import com.spotifylyrics.data.remote.api.LrclibApiService
import com.spotifylyrics.data.remote.api.LyricsOvhApiService
import com.spotifylyrics.data.remote.api.MusixmatchApiService
import com.spotifylyrics.domain.model.Lyrics
import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.domain.repository.LyricsRepository
import com.spotifylyrics.domain.util.LrcParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of LyricsRepository
 */
class LyricsRepositoryImpl @Inject constructor(
    private val lyricsDao: LyricsDao,
    private val lrclibApiService: LrclibApiService,
    private val geniusApiService: GeniusApiService,
    private val musixmatchApiService: MusixmatchApiService,
    private val lyricsOvhApiService: LyricsOvhApiService,
    private val settingsPreferences: com.spotifylyrics.data.local.preferences.SettingsPreferences
) : LyricsRepository {

    companion object {
        private const val SOURCE_LRCLIB = "lrclib"
        private const val SOURCE_GENIUS = "genius"
        private const val SOURCE_MUSIXMATCH = "musixmatch"
        private const val SOURCE_LYRICS_OVH = "lyrics_ovh"
        private const val SOURCE_USER = "user"
    }

    override suspend fun getLyrics(trackInfo: TrackInfo): Result<Lyrics> {
        // Check cache first
        val cached = lyricsDao.getLyricsByTrackAndArtistSync(trackInfo.track, trackInfo.artist)
        if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
            return Result.success(cached.toDomainModel())
        }

        // 1. Try LRCLIB first — only source providing synced (timestamped) lyrics
        lrclibApiService.fetchLyrics(trackInfo)?.let { lyrics ->
            cacheLyrics(lyrics, SOURCE_LRCLIB)
            return Result.success(lyrics)
        }

        // 2. Fall back to plain-lyrics scraping sources
        val plainSources = listOf(
            SOURCE_GENIUS to suspend { geniusApiService.fetchLyrics(trackInfo) },
            SOURCE_MUSIXMATCH to suspend { musixmatchApiService.fetchLyrics(trackInfo) },
            SOURCE_LYRICS_OVH to suspend { lyricsOvhApiService.fetchLyrics(trackInfo) }
        )

        for ((source, fetch) in plainSources) {
            try {
                fetch()?.let { lyrics ->
                    cacheLyrics(lyrics, source)
                    return Result.success(lyrics)
                }
            } catch (_: Exception) {
                continue
            }
        }

        return Result.failure(Exception("No lyrics found for ${trackInfo.artist} – ${trackInfo.track}"))
    }

    override fun getCachedLyrics(track: String, artist: String): Flow<Lyrics?> {
        return lyricsDao.getLyricsByTrackAndArtist(track, artist)
            .map { it?.toDomainModel() }
    }

    override suspend fun cacheLyrics(lyrics: Lyrics, source: String) {
        lyricsDao.insertLyrics(lyrics.toEntity(source))
    }

    override suspend fun clearCache() {
        lyricsDao.clearAllLyrics()
    }

    override suspend fun clearExpiredCache() {
        lyricsDao.deleteExpiredLyrics()
    }

    override fun getAllCachedLyrics(): Flow<List<Lyrics>> {
        return lyricsDao.getRecentLyrics(100).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchLyrics(query: String): Flow<List<Lyrics>> {
        return lyricsDao.searchLyrics(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun importLrcFile(track: String, artist: String, lrcContent: String): Result<Lyrics> {
        return try {
            val syncedLyrics = LrcParser.parse(lrcContent)
            val plainLyrics = syncedLyrics.joinToString("\n") { it.text }

            val lyrics = Lyrics(
                track = track,
                artist = artist,
                album = null,
                plainLyrics = plainLyrics,
                syncedLyrics = syncedLyrics
            )

            cacheLyrics(lyrics, SOURCE_USER)
            Result.success(lyrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToLrc(lyrics: Lyrics): Result<String> {
        return try {
            val lrcContent = if (lyrics.syncedLyrics != null) {
                lyrics.syncedLyrics.joinToString("\n") { line ->
                    val minutes = line.startTime / 60000
                    val seconds = (line.startTime % 60000) / 1000
                    val milliseconds = line.startTime % 1000
                    "[%02d:%02d.%02d]%s".format(minutes, seconds, milliseconds / 10, line.text)
                }
            } else {
                // Export plain lyrics without timestamps
                lyrics.plainLyrics
            }
            Result.success(lrcContent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
