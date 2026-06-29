package com.mliem.carlyrics.service.auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

class CarMainScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Now Playing Lyrics")
                    .addText("View synced lyrics for current track")
                    .setOnClickListener {
                        screenManager.push(CarLyricsScreen(carContext))
                    }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setSingleList(list)
            .setTitle("CarLyrics")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
