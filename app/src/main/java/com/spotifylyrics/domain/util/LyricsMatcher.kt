package com.mliem.carlyrics.domain.util

/**
 * Utility for fuzzy matching lyrics queries
 * Helps find lyrics when track/artist names don't exactly match
 */
object LyricsMatcher {

    /**
     * Calculate similarity between two strings using Levenshtein distance
     * Returns a value between 0 and 1, where 1 is exact match
     */
    fun calculateSimilarity(str1: String, str2: String): Float {
        val longer = if (str1.length > str2.length) str1 else str2
        val shorter = if (str1.length > str2.length) str2 else str1

        if (longer.isEmpty()) return 1.0f
        if (shorter.isEmpty()) return 0.0f

        val distance = levenshteinDistance(longer, shorter)
        val maxLength = longer.length.toFloat()

        return 1.0f - (distance / maxLength)
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) {
            dp[i][0] = i
        }
        for (j in 0..len2) {
            dp[0][j] = j
        }

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1, // deletion
                    dp[i][j - 1] + 1, // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Normalize a string for comparison (remove special chars, lowercase)
     */
    fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
    }

    /**
     * Check if two track/artist pairs likely match
     */
    fun isLikelyMatch(
        track1: String, artist1: String,
        track2: String, artist2: String,
        threshold: Float = 0.7f
    ): Boolean {
        val normalizedTrack1 = normalize(track1)
        val normalizedTrack2 = normalize(track2)
        val normalizedArtist1 = normalize(artist1)
        val normalizedArtist2 = normalize(artist2)

        val trackSimilarity = calculateSimilarity(normalizedTrack1, normalizedTrack2)
        val artistSimilarity = calculateSimilarity(normalizedArtist1, normalizedArtist2)

        // Require both track and artist to be reasonably similar
        return trackSimilarity >= threshold && artistSimilarity >= threshold
    }

    /**
     * Extract search terms from a combined query string
     * e.g., "Ed Sheeran Shape of You" -> ("Shape of You", "Ed Sheeran")
     */
    fun extractSearchTerms(query: String): Pair<String, String> {
        val parts = query.split(" - ", " by ", ":", limit = 2)
        return if (parts.size == 2) {
            // Try to detect which is artist and which is track
            val firstPart = parts[0].trim()
            val secondPart = parts[1].trim()

            // Usually "artist - track" or "track - artist"
            // Simple heuristic: if first part is short, it's likely the artist
            if (firstPart.split(" ").size <= 3) {
                secondPart to firstPart // track to artist
            } else {
                firstPart to secondPart
            }
        } else {
            // Can't parse, return as-is
            query to ""
        }
    }
}
