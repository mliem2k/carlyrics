package com.mliem.carlyrics.domain.usecase

import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.repository.LyricsRepository
import com.mliem.carlyrics.domain.util.LrcParser
import javax.inject.Inject

/**
 * Use case for importing LRC files
 */
class ImportLrcFileUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(
        track: String,
        artist: String,
        lrcContent: String
    ): Result<Lyrics> {
        if (!LrcParser.isValidLrc(lrcContent)) {
            return Result.failure(IllegalArgumentException("Invalid LRC file format"))
        }

        return lyricsRepository.importLrcFile(track, artist, lrcContent)
    }
}
