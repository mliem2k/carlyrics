package com.spotifylyrics.domain.util

import com.spotifylyrics.domain.model.SyncedLyricLine
import java.util.regex.Pattern

/**
 * Parser for LRC (Lyric) file format
 * LRC format: [mm:ss.ms]Lyric text
 */
object LrcParser {

    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d+):(\\d+)(?:\\.(\\d+))?\\]")

    /**
     * Parse LRC content into a list of synced lyric lines
     */
    fun parse(lrcContent: String): List<SyncedLyricLine> {
        val lines = lrcContent.lines()
        val syncedLyrics = mutableListOf<SyncedLyricLine>()

        for (line in lines) {
            val parsedLines = parseLine(line)
            syncedLyrics.addAll(parsedLines)
        }

        return syncedLyrics.sortedBy { it.startTime }
    }

    /**
     * Parse a single LRC line
     * Can contain multiple time tags for the same lyric
     */
    private fun parseLine(line: String): List<SyncedLyricLine> {
        val result = mutableListOf<SyncedLyricLine>()
        val matcher = TIME_TAG_PATTERN.matcher(line)
        val timeTags = mutableListOf<Long>()
        var lastMatchEnd = 0

        // Find all time tags
        while (matcher.find()) {
            val minutes = matcher.group(1)?.toLong() ?: 0
            val seconds = matcher.group(2)?.toLong() ?: 0
            val milliseconds = matcher.group(3)?.toLong()?.let { it * 10 } ?: 0 // Convert to ms

            val timeMs = minutes * 60000 + seconds * 1000 + milliseconds
            timeTags.add(timeMs)
            lastMatchEnd = matcher.end()
        }

        // Extract lyric text (after last time tag)
        val lyricText = line.substring(lastMatchEnd).trim()

        // Create synced lyric lines for each time tag
        if (timeTags.isNotEmpty() && lyricText.isNotEmpty()) {
            timeTags.forEach { timeMs ->
                result.add(SyncedLyricLine(timeMs, lyricText))
            }
        }

        return result
    }

    /**
     * Convert synced lyrics back to LRC format
     */
    fun toLrcFormat(syncedLyrics: List<SyncedLyricLine>): String {
        return syncedLyrics.joinToString("\n") { line ->
            val minutes = line.startTime / 60000
            val seconds = (line.startTime % 60000) / 1000
            val milliseconds = (line.startTime % 1000) / 10
            "[%02d:%02d.%02d]%s".format(minutes, seconds, milliseconds, line.text)
        }
    }

    /**
     * Validate LRC format
     */
    fun isValidLrc(content: String): Boolean {
        return TIME_TAG_PATTERN.matcher(content).find()
    }
}
