package com.spotifylyrics.service.auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.spotifylyrics.service.auto.screen.CarLyricsScreen

/**
 * Main screen for Android Auto
 */
class CarMainScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        listBuilder.addItem(
            Row.Builder()
                .setTitle("View Lyrics")
                .setOnClickListener { screenManager.push(CarLyricsScreen(carContext)) }
                .build()
        )

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle("Spotify Lyrics")
            .build()
    }
}
