package com.spotifylyrics.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spotifylyrics.data.local.database.dao.LyricsDao
import com.spotifylyrics.data.local.database.dao.TrackInfoDao
import com.spotifylyrics.data.local.database.entity.CachedLyricsEntity
import com.spotifylyrics.data.local.database.entity.TrackInfoEntity

/**
 * Room database for Spotify Lyrics app
 * Contains tables for cached lyrics and track history
 */
@Database(
    entities = [
        CachedLyricsEntity::class,
        TrackInfoEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LyricsDatabase : RoomDatabase() {

    /**
     * DAO for lyrics operations
     */
    abstract fun lyricsDao(): LyricsDao

    /**
     * DAO for track history operations
     */
    abstract fun trackInfoDao(): TrackInfoDao
}
