package com.spotifylyrics.domain.usecase

import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting current track info
 */
class GetCurrentTrackUseCase @Inject constructor(
    private val trackRepository: TrackRepository
) {
    operator fun invoke(): Flow<TrackInfo?> {
        return trackRepository.getCurrentTrackInfo()
    }
}
