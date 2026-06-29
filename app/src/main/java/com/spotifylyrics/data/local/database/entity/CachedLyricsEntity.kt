package com.mliem.carlyrics.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mliem.carlyrics.domain.model.Lyrics

/**
 * Room entity for cached lyrics
 */
@Entity(
    tableName = "cached_lyrics",
    indices = [Index(value = ["track", "artist"], unique = true)]
)
data class CachedLyricsEntity(
    @PrimaryKey val id: String,
    val track: String,
    val artist: String,
    val album: String?,
    val lyrics: String,
    val source: String,
    val isSynced: Boolean = false,
    val syncedLyrics: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
)

/**
 * Extension function to convert domain model to entity
 */
fun Lyrics.toEntity(source: String): CachedLyricsEntity {
    return CachedLyricsEntity(
        id = "${track}_$artist",
        track = track,
        artist = artist,
        album = album,
        lyrics = plainLyrics,
        source = source,
        isSynced = syncedLyrics != null,
        syncedLyrics = syncedLyrics?.let { syncLyrics ->
            syncLyrics.joinToString("\n") { "[${it.startTime}]${it.text}" }
        }
    )
}

/**
 * Extension function to convert entity to domain model
 */
fun CachedLyricsEntity.toDomainModel(): Lyrics {
    return Lyrics(
        track = track,
        artist = artist,
        album = album,
        plainLyrics = lyrics,
        syncedLyrics = syncedLyrics?.let { syncLyricsJson ->
            // Parse synced lyrics from LRC-like format
            parseSyncedLyrics(syncLyricsJson)
        }
    )
}

/**
 * Parse synced lyrics from string format
 */
private val SYNCED_TIME_REGEX = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\]")

private fun parseSyncedLyrics(syncedLyricsJson: String): List<com.mliem.carlyrics.domain.model.SyncedLyricLine> {
    return syncedLyricsJson.lines()
        .mapNotNull { line ->
            val match = SYNCED_TIME_REGEX.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val milliseconds = match.groupValues[3].toLong()
                val startTime = minutes * 60000 + seconds * 1000 + milliseconds
                val text = line.substring(match.range.last + 1)
                com.mliem.carlyrics.domain.model.SyncedLyricLine(startTime, text)
            } else {
                null
            }
        }
}
