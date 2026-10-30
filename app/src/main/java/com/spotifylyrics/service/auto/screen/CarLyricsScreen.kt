package com.mliem.carlyrics.service.auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo

/**
 * Root Android Auto screen. A ListTemplate is the closest fit the host offers for lyrics:
 * the line being sung is marked, one line before it keeps context, and the rest are upcoming.
 */
class CarLyricsScreen(carContext: CarContext) : Screen(carContext) {

    private var track: TrackInfo? = null
    private var lyrics: Lyrics? = null
    private var lineIndex: Int = -1

    // The host caps list rows (and lowers the cap while driving), so ask instead of assuming.
    private val maxRows = carContext.getCarService(ConstraintManager::class.java)
        .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
        .coerceIn(1, MAX_ROWS)

    fun update(track: TrackInfo?, lyrics: Lyrics?, lineIndex: Int) {
        this.track = track
        this.lyrics = lyrics
        this.lineIndex = lineIndex
        invalidate()
    }

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        val synced = lyrics?.syncedLyrics.orEmpty()

        when {
            track == null -> list.addItem(message("Waiting for music", "Play a song on your phone and the lyrics appear here."))
            lyrics == null -> list.addItem(message("Finding lyrics", "Looking up the lyrics for this song."))
            synced.isEmpty() -> {
                list.addItem(message("Lyrics are not time synced", "Showing the first lines only."))
                lyrics?.plainLyrics.orEmpty().lines().filter { it.isNotBlank() }.take(maxRows - 1)
                    .forEach { list.addItem(Row.Builder().setTitle(it).build()) }
            }
            else -> {
                val current = lineIndex.coerceIn(0, synced.lastIndex)
                val start = (current - 1).coerceAtLeast(0)
                val end = (start + maxRows - 1).coerceAtMost(synced.lastIndex)
                for (i in start..end) {
                    val text = synced[i].text.ifBlank { "♪" }
                    list.addItem(Row.Builder().setTitle(if (i == current) "▶ $text" else text).build())
                }
            }
        }

        return ListTemplate.Builder()
            .setSingleList(list.build())
            .setTitle(track?.let { "${it.track}, ${it.artist}" } ?: "CarLyrics")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    private fun message(title: String, text: String) = Row.Builder().setTitle(title).addText(text).build()

    private companion object {
        const val MAX_ROWS = 5
    }
}
