package com.mliem.carlyrics.service.media

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/** The two colors that best describe a cover, as ARGB. The same color twice when the cover has one. */
data class ArtworkPalette(val primary: Int, val secondary: Int)

private const val LEVELS = 8
private const val MIN_SECOND_COLOR_DISTANCE = 90.0

/**
 * Finds a cover's main color and a clearly different second one from its pixels. Pixels fall into
 * 8x8x8 color buckets, and saturated buckets count extra so a vivid accent beats a large grey area.
 * Returns null for an empty image.
 */
internal fun extractPalette(pixels: IntArray): ArtworkPalette? {
    if (pixels.isEmpty()) return null
    val buckets = LEVELS * LEVELS * LEVELS
    val score = DoubleArray(buckets)
    val count = IntArray(buckets)
    val sums = Array(3) { LongArray(buckets) }

    for (pixel in pixels) {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val key = ((r shr 5) shl 6) or ((g shr 5) shl 3) or (b shr 5)
        val max = maxOf(r, g, b)
        val saturation = if (max == 0) 0.0 else (max - minOf(r, g, b)).toDouble() / max
        score[key] += 0.25 + saturation
        count[key]++
        sums[0][key] += r.toLong()
        sums[1][key] += g.toLong()
        sums[2][key] += b.toLong()
    }

    fun colorOf(key: Int): Int {
        val n = count[key]
        return (0xFF shl 24) or ((sums[0][key] / n).toInt() shl 16) or
            ((sums[1][key] / n).toInt() shl 8) or (sums[2][key] / n).toInt()
    }

    val ranked = (0 until buckets).filter { count[it] > 0 }.sortedByDescending { score[it] }
    val primary = colorOf(ranked.first())
    val secondary = ranked.drop(1).map(::colorOf).firstOrNull { distance(it, primary) > MIN_SECOND_COLOR_DISTANCE }
    return ArtworkPalette(primary, secondary ?: primary)
}

private fun distance(a: Int, b: Int): Double {
    val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
    val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
    val db = (a and 0xFF) - (b and 0xFF)
    return sqrt((dr * dr + dg * dg + db * db).toDouble())
}

/**
 * Turns a cover color into a backdrop color: same hue, capped brightness. Dark enough that white lyrics
 * (even the dimmed lines) stay readable on any cover, bright enough to still look like the cover.
 */
internal fun backdropColor(argb: Int, maxValue: Float = 0.42f, maxSaturation: Float = 0.85f): Int {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val delta = max - minOf(r, g, b)
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val saturation = (if (max == 0f) 0f else delta / max).coerceAtMost(maxSaturation)
    val value = max.coerceAtMost(maxValue)

    val c = value * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = value - c
    val (r1, g1, b1) = when (floor(hue / 60f).toInt() % 6) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun channel(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (channel(r1) shl 16) or (channel(g1) shl 8) or channel(b1)
}
