package com.spotifylyrics.domain.usecase

import com.spotifylyrics.domain.model.Lyrics
import com.spotifylyrics.domain.model.TrackInfo
import com.spotifylyrics.domain.repository.LyricsRepository
import javax.inject.Inject

/**
 * Use case for fetching lyrics for a track
 */
class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(trackInfo: TrackInfo): Result<Lyrics> {
        if (trackInfo.track.isBlank() || trackInfo.artist.isBlank()) {
            return Result.failure(IllegalArgumentException("Track and artist cannot be empty"))
        }
        return lyricsRepository.getLyrics(trackInfo)
    }
}
