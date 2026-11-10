package com.mliem.carlyrics.service.media

import android.graphics.Bitmap
import com.mliem.carlyrics.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Side of a cover's thumbnail that palette extraction reads. Colors need no more. */
private const val ARTWORK_SIZE_PX = 32

/**
 * Turns a cover into its two main colors. Never call this on the main thread: it scales and
 * walks [ARTWORK_SIZE_PX] squared pixels.
 */
internal fun decodeArtworkPalette(bitmap: Bitmap): ArtworkPalette? {
    if (bitmap.isRecycled) return null
    val thumbnail = Bitmap.createScaledBitmap(bitmap, ARTWORK_SIZE_PX, ARTWORK_SIZE_PX, true)
    return try {
        val pixels = IntArray(ARTWORK_SIZE_PX * ARTWORK_SIZE_PX)
        thumbnail.getPixels(pixels, 0, ARTWORK_SIZE_PX, 0, 0, ARTWORK_SIZE_PX, ARTWORK_SIZE_PX)
        extractPalette(pixels)
    } finally {
        // createScaledBitmap returns the source itself when it already has the requested size.
        if (thumbnail !== bitmap) thumbnail.recycle()
    }
}

/**
 * Runs one artwork computation at a time and only keeps the latest submission. A queued
 * computation for a track the user already left is cancelled, and a result that finishes
 * after a newer submission or a [clear] is dropped instead of delivered.
 *
 * [submit] and [clear] are expected on one thread (the main thread); [onResult] runs on the
 * scope's dispatcher, which is [kotlinx.coroutines.Dispatchers.Default] in the app.
 */
internal class LatestArtworkJob<Input>(
    private val scope: CoroutineScope,
    private val compute: suspend (Input) -> ArtworkPalette?
) {
    // Serializes submissions, clearing and delivery so a result cannot land after a newer
    // submission or a clear has returned.
    private val lock = Any()

    /** Key of the in-flight computation, or null when nothing is running. */
    private var activeKey: String? = null

    /** Incremented per submission and by [clear] so late results can tell they are stale. */
    private var generation = 0L

    private var job: Job? = null

    fun submit(key: String, input: Input, onResult: (ArtworkPalette?) -> Unit) {
        synchronized(lock) {
            // Playback updates republish the same track; the running computation already covers it.
            if (key == activeKey) return
            job?.cancel()
            val submission = ++generation
            activeKey = key
            job = scope.launch {
                try {
                    val palette = try {
                        compute(input)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // A player can recycle its metadata bitmap while it is being read.
                        null
                    }
                    synchronized(lock) {
                        if (submission == generation) onResult(palette)
                    }
                } finally {
                    synchronized(lock) {
                        if (submission == generation) activeKey = null
                    }
                }
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            generation++
            activeKey = null
            job?.cancel()
            job = null
        }
    }
}

/** Shared scheduler for cover palettes, computed off the main thread. */
@Singleton
class ArtworkPaletteLoader @Inject constructor(
    @ApplicationScope scope: CoroutineScope
) {
    private val latest = LatestArtworkJob<Bitmap>(scope) { decodeArtworkPalette(it) }

    /** Replaces any pending extraction. [onResult] runs off the main thread. */
    fun submit(key: String, bitmap: Bitmap, onResult: (ArtworkPalette?) -> Unit) {
        latest.submit(key, bitmap, onResult)
    }

    /** Cancels pending work and drops any result that is still in flight. */
    fun clear() = latest.clear()
}
