package com.mliem.carlyrics.data.repository

import com.mliem.carlyrics.data.local.database.dao.TrackInfoDao
import com.mliem.carlyrics.data.local.database.entity.toDomainModel
import com.mliem.carlyrics.data.local.database.entity.toEntity
import com.mliem.carlyrics.domain.model.TrackInfo
import com.mliem.carlyrics.domain.repository.TrackRepository
import com.mliem.carlyrics.service.notification.TrackInfoEmitter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of TrackRepository
 */
class TrackRepositoryImpl @Inject constructor(
    private val trackInfoDao: TrackInfoDao,
    private val trackInfoEmitter: TrackInfoEmitter
) : TrackRepository {

    override fun getCurrentTrackInfo(): Flow<TrackInfo?> {
        return trackInfoEmitter.trackInfoFlow
    }

    override fun getRecentTracks(limit: Int): Flow<List<TrackInfo>> {
        return trackInfoDao.getRecentTracks(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun saveTrackToHistory(trackInfo: TrackInfo) {
        trackInfoDao.insertTrack(trackInfo.toEntity())
    }

    override fun getTrackCount(): Flow<Int> {
        return trackInfoDao.getTrackCount()
    }

    override suspend fun clearHistory() {
        trackInfoDao.clearAllTracks()
    }

    override fun getAllArtists(): Flow<List<String>> {
        return trackInfoDao.getAllArtists()
    }
}
