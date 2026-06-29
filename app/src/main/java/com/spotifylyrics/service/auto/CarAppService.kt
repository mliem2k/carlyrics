package com.mliem.carlyrics.service.auto

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.mliem.carlyrics.service.auto.session.CarLyricsSession

/**
 * Car App Service for Android Auto integration
 */
class CarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // For development, allow all hosts
        // In production, you should restrict to specific hosts
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return CarLyricsSession()
    }
}
