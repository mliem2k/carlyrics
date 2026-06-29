package com.spotifylyrics.service.auto.session

import androidx.car.app.Screen
import androidx.car.app.Session
import com.spotifylyrics.service.auto.screen.CarMainScreen

/**
 * Session for Android Auto app
 */
class CarLyricsSession : Session() {

    override fun onCreateScreen(intent: android.content.Intent): Screen {
        return CarMainScreen(carContext)
    }
}
