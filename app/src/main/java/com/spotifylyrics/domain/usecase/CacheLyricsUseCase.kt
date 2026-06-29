package com.spotifylyrics.domain.usecase

import com.spotifylyrics.domain.model.Lyrics
import com.spotifylyrics.domain.repository.LyricsRepository
import javax.inject.Inject

/**
 * Use case for caching lyrics
 */
class CacheLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(lyrics: Lyrics, source: String = "user") {
        lyricsRepository.cacheLyrics(lyrics, source)
    }
}
