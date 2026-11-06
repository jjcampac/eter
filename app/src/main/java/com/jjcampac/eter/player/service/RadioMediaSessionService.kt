package com.jjcampac.eter.player.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jjcampac.eter.di.AppModule
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.PlayerState
import com.jjcampac.eter.player.RadioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Servicio de sesión de medios que permite la reproducción en segundo plano.
 *
 * Se ejecuta como servicio en primer plano (`mediaPlayback`) con una
 * notificación persistente que contiene play/pausa y siguiente/anterior
 * entre favoritas (estos últimos operativos a partir de la Fase 3).
 */
class RadioMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var radioPlayer: RadioPlayer? = null
    private var notificationManager: NotificationManagerCompat? = null
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        radioPlayer = AppModule.radioPlayer(this)
        val player = radioPlayer!!.exoPlayer
        mediaSession = MediaSession.Builder(this, player).build()
        notificationManager = NotificationManagerCompat.from(this)
        ensureNotificationChannel(this)

        // Observar cambios de estado del reproductor para actualizar la notificación.
        scope.launch {
            radioPlayer?.state?.collect { state ->
                updateNotification(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_PLAY_PAUSE -> togglePlay()
            ACTION_NEXT -> /* siguiente favorita (Fase 3) */ Unit
            ACTION_PREVIOUS -> /* anterior favorita (Fase 3) */ Unit
            ACTION_STOP -> stopPlayback()
            null -> Unit
        }

        // Si hay reproducción activa, mantener en primer plano.
        val state = radioPlayer?.state?.value
        if (state?.status == PlaybackStatus.PLAYING || state?.status == PlaybackStatus.PAUSED) {
            updateNotification(state)
            startForeground(NOTIFICATION_ID, buildNotification(this, state.currentStation, state.status, state.metadataTitle))
        }

        return Service.START_STICKY
    }

    override fun onGetSession(controllerInfo: androidx.media3.session.MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        job.cancel()
        stopForeground(true)
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun togglePlay() {
        val state = radioPlayer?.state?.value
        val station = state?.currentStation ?: return
        when (state?.status) {
            PlaybackStatus.PLAYING -> radioPlayer?.pause()
            PlaybackStatus.PAUSED -> radioPlayer?.resume()
            else -> radioPlayer?.play(station)
        }
    }

    private fun stopPlayback() {
        radioPlayer?.stop()
        stopForeground(true)
    }

    private fun updateNotification(state: PlayerState) {
        val notification = buildNotification(
            this,
            state.currentStation,
            state.status,
            state.metadataTitle
        )
        notificationManager?.notify(NOTIFICATION_ID, notification)

        // Si está reproduciendo o pausado, mantener primero plano.
        if (state.status == PlaybackStatus.PLAYING || state.status == PlaybackStatus.PAUSED) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            stopForeground(true)
        }
    }
}
