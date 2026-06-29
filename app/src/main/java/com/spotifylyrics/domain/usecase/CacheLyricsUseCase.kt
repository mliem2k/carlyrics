package com.mliem.carlyrics.domain.usecase

import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.repository.LyricsRepository
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
