package com.spotifylyrics.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spotifylyrics.data.local.database.entity.TrackInfoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for track history operations
 */
@Dao
interface TrackInfoDao {

    @Query("SELECT * FROM track_history ORDER BY playedAt DESC LIMIT 1")
    fun getLastPlayedTrack(): Flow<TrackInfoEntity?>

    @Query("SELECT * FROM track_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentTracks(limit: Int = 50): Flow<List<TrackInfoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackInfoEntity)

    @Query("DELETE FROM track_history WHERE playedAt < :timestamp")
    suspend fun deleteOldTracks(timestamp: Long): Int

    @Query("DELETE FROM track_history")
    suspend fun clearAllTracks(): Int

    @Query("SELECT COUNT(*) FROM track_history")
    fun getTrackCount(): Flow<Int>

    @Query("SELECT DISTINCT artist FROM track_history ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT * FROM track_history WHERE artist = :artist ORDER BY playedAt DESC")
    fun getTracksByArtist(artist: String): Flow<List<TrackInfoEntity>>
}
