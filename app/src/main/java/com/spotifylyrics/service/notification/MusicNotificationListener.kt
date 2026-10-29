package com.mliem.carlyrics.service.notification

import android.service.notification.NotificationListenerService

/**
 * Intentionally empty. Enabling notification access for this component is what
 * lets [com.mliem.carlyrics.service.media.MediaSessionManager] read the active media sessions.
 */
class MusicNotificationListener : NotificationListenerService()
