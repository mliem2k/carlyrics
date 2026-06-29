package com.spotifylyrics.service.auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

/**
 * Screen for displaying lyrics in Android Auto
 */
class CarLyricsScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // TODO: Load actual lyrics from repository
        listBuilder.addItem(
            Row.Builder()
                .setTitle("No lyrics loaded")
                .addText("Play music on your phone to see lyrics")
                .build()
        )

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(CarIcon.APP_ICON)
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle("Lyrics")
            .setActionStrip(actionStrip)
            .build()
    }
}
