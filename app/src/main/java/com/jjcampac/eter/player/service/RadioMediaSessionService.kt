package com.jjcampac.eter.player.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jjcampac.eter.di.AppModule

/**
 * Servicio de sesión de medios que permite la reproducción en segundo plano.
 *
 * En la Fase 1 mantiene la sesión mínima compartida con la UI.
 * En la Fase 2 se completa con la notificación de reproducción
 * (play/pausa y siguiente/anterior entre favoritas).
 */
class RadioMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = AppModule.radioPlayer(this).exoPlayer
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
