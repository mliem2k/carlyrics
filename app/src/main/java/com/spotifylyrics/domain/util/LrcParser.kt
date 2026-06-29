package com.spotifylyrics.domain.util

import com.spotifylyrics.domain.model.SyncedLyricLine

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

        val timeTags = matches.map { match ->
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val millis = match.groupValues[3].takeIf { it.isNotEmpty() }?.toLong()?.times(10) ?: 0L
            minutes * 60_000 + seconds * 1_000 + millis
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
