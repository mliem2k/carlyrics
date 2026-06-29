package com.mliem.carlyrics.service.auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.mliem.carlyrics.domain.model.Lyrics
import com.mliem.carlyrics.domain.model.TrackInfo

class CarLyricsScreen(
    carContext: CarContext,
    private var currentTrack: TrackInfo? = null,
    private var currentLyrics: Lyrics? = null,
    private var currentPositionMs: Long = 0L
) : Screen(carContext) {

    fun update(track: TrackInfo?, lyrics: Lyrics?, positionMs: Long) {
        currentTrack = track
        currentLyrics = lyrics
        currentPositionMs = positionMs
        invalidate()
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val track = currentTrack
        val lyrics = currentLyrics

        if (track == null || lyrics == null) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("No lyrics loaded")
                    .addText("Play music on your phone to see lyrics")
                    .build()
            )
        } else {
            val synced = lyrics.syncedLyrics
            if (synced != null && synced.isNotEmpty()) {
                val currentIndex = lyrics.getCurrentLyricIndex(currentPositionMs)
                    .coerceAtLeast(0)

                val start = (currentIndex - 2).coerceAtLeast(0)
                val end = (currentIndex + 2).coerceAtMost(synced.size - 1)

                for (i in start..end) {
                    val line = synced[i]
                    val isCurrent = i == currentIndex
                    listBuilder.addItem(
                        Row.Builder()
                            .setTitle(if (isCurrent) "▶ ${line.text}" else line.text)
                            .build()
                    )
                }
            } else {
                // Plain lyrics — show first 5 lines
                val lines = lyrics.plainLyrics.lines().filter { it.isNotBlank() }.take(5)
                lines.forEach { line ->
                    listBuilder.addItem(Row.Builder().setTitle(line).build())
                }
            }
        }

        val actionStrip = ActionStrip.Builder()
            .addAction(Action.BACK)
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle(currentTrack?.let { "${it.artist} – ${it.track}" } ?: "CarLyrics")
            .setActionStrip(actionStrip)
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
