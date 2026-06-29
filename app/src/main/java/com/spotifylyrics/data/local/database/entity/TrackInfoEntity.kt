package com.spotifylyrics.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spotifylyrics.domain.model.TrackInfo

/**
 * Room entity for track playback history
 */
@Entity(tableName = "track_history")
data class TrackInfoEntity(
    @PrimaryKey val id: String,
    val track: String,
    val artist: String,
    val album: String?,
    val playedAt: Long = System.currentTimeMillis(),
    val isPlaying: Boolean = false
)

/**
 * Extension function to convert domain model to entity
 */
fun TrackInfo.toEntity(): TrackInfoEntity {
    val playedAt = System.currentTimeMillis()
    return TrackInfoEntity(
        id = "${track}_${artist}_$playedAt",
        track = track,
        artist = artist,
        album = album,
        playedAt = playedAt,
        isPlaying = isPlaying
    )
}

/**
 * Extension function to convert entity to domain model
 */
fun TrackInfoEntity.toDomainModel(): TrackInfo {
    return TrackInfo(
        track = track,
        artist = artist,
        album = album,
        isPlaying = isPlaying
    )
}
