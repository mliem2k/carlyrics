package com.mliem.carlyrics.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mliem.carlyrics.data.local.database.entity.CachedLyricsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cached lyrics operations
 */
@Dao
interface LyricsDao {

    @Query("SELECT * FROM cached_lyrics WHERE track = :track AND artist = :artist LIMIT 1")
    fun getLyricsByTrackAndArtist(track: String, artist: String): Flow<CachedLyricsEntity?>

    @Query("SELECT * FROM cached_lyrics WHERE track = :track AND artist = :artist LIMIT 1")
    suspend fun getLyricsByTrackAndArtistSync(track: String, artist: String): CachedLyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: CachedLyricsEntity)

    @Query("DELETE FROM cached_lyrics WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredLyrics(currentTime: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM cached_lyrics")
    suspend fun clearAllLyrics(): Int

    @Query("SELECT * FROM cached_lyrics ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentLyrics(limit: Int = 20): Flow<List<CachedLyricsEntity>>

    @Query("SELECT COUNT(*) FROM cached_lyrics")
    fun getLyricsCount(): Flow<Int>

    @Query("SELECT * FROM cached_lyrics WHERE track LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchLyrics(query: String): Flow<List<CachedLyricsEntity>>
}
