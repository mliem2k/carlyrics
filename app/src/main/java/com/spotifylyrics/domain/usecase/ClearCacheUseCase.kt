package com.mliem.carlyrics.domain.usecase

import com.mliem.carlyrics.domain.repository.LyricsRepository
import javax.inject.Inject

/**
 * Use case for clearing cached lyrics
 */
class ClearCacheUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(expiredOnly: Boolean = false) {
        if (expiredOnly) {
            lyricsRepository.clearExpiredCache()
        } else {
            lyricsRepository.clearCache()
        }
    }
}
