package com.mliem.carlyrics.service.auto.screen

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mliem.carlyrics.R
import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo

/**
 * Root Android Auto screen. It is a navigation style screen that draws the lyrics itself, so the
 * host gives it the main pane and keeps the media card (Spotify and others) visible next to it,
 * the way it does for a maps app. The line being sung is bright, the lines around it are dimmed.
 */
class CarLyricsScreen(
    carContext: CarContext,
    private val onPrevious: () -> Unit,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit
) : Screen(carContext) {

    private var track: TrackInfo? = null
    private var lyrics: Lyrics? = null
    private var lineIndex: Int = -1
    private var backdrop: Int = Color.BLACK

    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var density = 1f

    // The part of the surface no host overlay (clock, buttons) covers.
    private var safeArea: Rect? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
            }
        })
    }

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            surface = surfaceContainer.surface
            width = surfaceContainer.width
            height = surfaceContainer.height
            density = surfaceContainer.dpi / 160f
            draw()
        }

        override fun onVisibleAreaChanged(visibleArea: Rect) {
            safeArea = visibleArea
            draw()
        }

        override fun onStableAreaChanged(stableArea: Rect) {
            safeArea = stableArea
            draw()
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            surface = null
        }
    }

    fun update(track: TrackInfo?, lyrics: Lyrics?, lineIndex: Int, backdrop: Int) {
        this.track = track
        this.lyrics = lyrics
        this.lineIndex = lineIndex
        this.backdrop = backdrop
        draw()
    }

    override fun onGetTemplate(): Template {
        fun control(icon: Int, action: () -> Unit) = Action.Builder()
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, icon)).build())
            .setOnClickListener(action)
            .build()

        return NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.createCustom(Color.BLACK, Color.BLACK))
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(control(R.drawable.ic_car_prev, onPrevious))
                    .addAction(control(R.drawable.ic_car_play_pause, onPlayPause))
                    .addAction(control(R.drawable.ic_car_next, onNext))
                    .build()
            )
            .build()
    }

    private fun draw() {
        val target = surface?.takeIf { it.isValid } ?: return
        val canvas = try {
            target.lockCanvas(null)
        } catch (e: IllegalStateException) {
            return
        } catch (e: IllegalArgumentException) {
            return
        }
        try {
            render(canvas)
        } finally {
            target.unlockCanvasAndPost(canvas)
        }
    }

    private fun render(canvas: Canvas) {
        val area = safeArea?.takeIf { !it.isEmpty } ?: Rect(0, 0, width, height)
        val pad = 24 * density
        val left = area.left + pad
        val contentWidth = (area.width() - 2 * pad).toInt().coerceAtLeast(1)

        // Same look as the phone: the cover's color fading to black.
        val background = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), backdrop, Color.BLACK, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

        var y = area.top + pad
        track?.let {
            y = drawText(canvas, it.track, left, y, contentWidth, 30f, Color.WHITE, bold = true, maxLines = 1)
            y = drawText(canvas, it.artist, left, y, contentWidth, 22f, Color.argb(178, 255, 255, 255), maxLines = 1)
        }
        y += 12 * density

        val synced = lyrics?.syncedLyrics.orEmpty()
        when {
            track == null -> message(canvas, "Waiting for music", "Play a song on your phone and the lyrics appear here.", left, y, contentWidth)
            lyrics == null -> message(canvas, "Finding lyrics", "Looking up the lyrics for this song.", left, y, contentWidth)
            synced.isEmpty() -> message(canvas, "Lyrics are not time synced", "Open the app on your phone to read them.", left, y, contentWidth)
            else -> drawLyrics(canvas, synced.map { it.text.ifBlank { "♪" } }, lineIndex.coerceIn(0, synced.lastIndex), left, y, contentWidth, area.bottom - pad)
        }
    }

    private fun drawLyrics(canvas: Canvas, lines: List<String>, current: Int, left: Float, top: Float, contentWidth: Int, bottom: Float) {
        // One line of context before the current one, then as many upcoming lines as fit.
        var y = top
        var index = (current - 1).coerceAtLeast(0)
        while (index < lines.size && y < bottom) {
            val isCurrent = index == current
            val size = if (isCurrent) 46f else 32f
            val alpha = when {
                isCurrent -> 255
                index < current -> 97
                else -> 140
            }
            y = drawText(canvas, lines[index], left, y, contentWidth, size, Color.argb(alpha, 255, 255, 255), bold = true, maxLines = 2)
            y += 10 * density
            index++
        }
    }

    private fun message(canvas: Canvas, title: String, detail: String, left: Float, top: Float, contentWidth: Int) {
        val afterTitle = drawText(canvas, title, left, top + 24 * density, contentWidth, 38f, Color.WHITE, bold = true)
        drawText(canvas, detail, left, afterTitle + 8 * density, contentWidth, 26f, Color.argb(178, 255, 255, 255))
    }

    /** Draws wrapped text with its top at [top] and returns the y just under it. */
    private fun drawText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        contentWidth: Int,
        sizeDp: Float,
        color: Int,
        bold: Boolean = false,
        maxLines: Int = 4
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sizeDp * density
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .build()
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }
}
