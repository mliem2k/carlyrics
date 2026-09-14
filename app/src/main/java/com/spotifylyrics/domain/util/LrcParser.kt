package com.mliem.carlyrics.domain.util

import com.mliem.carlyrics.domain.model.SyncedLyricLine

object LrcParser {

    private val TIME_TAG_REGEX = Regex("\\[(\\d+):(\\d+)(?:\\.(\\d+))?\\]")

    fun parse(lrcContent: String): List<SyncedLyricLine> {
        return lrcContent.lines()
            .flatMap { parseLine(it) }
            .sortedBy { it.startTime }
    }

    private fun parseLine(line: String): List<SyncedLyricLine> {
        val matches = TIME_TAG_REGEX.findAll(line).toList()
        if (matches.isEmpty()) return emptyList()

        val timeTags = try {
            matches.map { match ->
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                // Pad/truncate the fractional group to exactly 3 digits
                // (milliseconds) instead of assuming it is always 2-digit
                // centiseconds, so 1-digit tenths ([00:01.5]) and 3-digit
                // milliseconds ([00:01.234]) both scale correctly.
                val fracDigits = match.groupValues[3]
                val millis = if (fracDigits.isEmpty()) 0L else fracDigits.padEnd(3, '0').take(3).toLong()
                minutes * 60_000 + seconds * 1_000 + millis
            }
        } catch (_: NumberFormatException) {
            // An unreasonably long digit run (malformed/adversarial input)
            // overflows Long; skip this line rather than crash the parse.
            return emptyList()
        }

        val lyricText = line.substring(matches.last().range.last + 1).trim()
        if (lyricText.isEmpty()) return emptyList()

        return timeTags.map { SyncedLyricLine(it, lyricText) }
    }

    fun toLrcFormat(syncedLyrics: List<SyncedLyricLine>): String {
        return syncedLyrics.joinToString("\n") { line ->
            val minutes = line.startTime / 60_000
            val seconds = (line.startTime % 60_000) / 1_000
            val millis = (line.startTime % 1_000) / 10
            "[%02d:%02d.%02d]%s".format(minutes, seconds, millis, line.text)
        }
    }

    fun isValidLrc(content: String): Boolean = TIME_TAG_REGEX.containsMatchIn(content)
}
