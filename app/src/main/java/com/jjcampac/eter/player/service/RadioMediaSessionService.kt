package com.jjcampac.eter.player.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jjcampac.eter.EterApplication
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.PlayerState
import com.jjcampac.eter.player.RadioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val TAG = "RadioMediaSessionService"

/**
 * Servicio de sesión de medios que permite la reproducción en segundo plano.
 *
 * Se ejecuta como servicio en primer plano (`mediaPlayback`) con una
 * notificación persistente que contiene play/pausa y siguiente/anterior
 * entre favoritas (estos últimos operativos a partir de la Fase 3).
 *
 * El [RadioPlayer] se obtiene de [EterApplication] para sobrevivir a la
 * recreación de la actividad y al reinicio del servicio. Si el proceso fue
 * muerto por el sistema, al iniciar el servicio se auto-reanuda la última
 * emisora guardada en SharedPreferences.
 *
 * Las acciones de la notificación, el ID de notificación, el canal y la
 * construcción de la notificación viven en [PlayerNotification] (mismo
 * paquete) para no duplicar definiciones.
 */
class RadioMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var radioPlayer: RadioPlayer
    private var notificationManager: android.app.NotificationManager? = null
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        val app = application as EterApplication
        radioPlayer = app.getRadioPlayer()
        val player = radioPlayer.exoPlayer
        mediaSession = MediaSession.Builder(this, player).build()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        ensureNotificationChannel(this)

        // Observar cambios de estado del reproductor para actualizar la notificación.
        scope.launch {
            radioPlayer.state.collect { state ->
                Log.d(TAG, "state collected: status=${state.status} station=${state.currentStation?.name} metadata=${state.metadataTitle}")
                updateNotification(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand() action=${intent?.action}")
        val action = intent?.action
        when (action) {
            ACTION_PLAY_PAUSE -> togglePlay()
            ACTION_NEXT -> /* siguiente favorita (Fase 3) */ Unit
            ACTION_PREVIOUS -> /* anterior favorita (Fase 3) */ Unit
            ACTION_STOP -> stopPlayback()
            null -> Unit
        }

        // Con estacion cargada, mantener siempre en primer plano para que
        // Android no mate el proceso aunque vengamos de la lista de emisoras.
        val currentState = radioPlayer.state.value
        if (currentState.currentStation != null) {
            updateNotification(currentState)
            startForeground(NOTIFICATION_ID, buildNotification(this, currentState.currentStation, currentState.status, currentState.metadataTitle))
        } else {
            stopForeground(true)
        }
        return Service.START_STICKY
    }

    override fun onGetSession(controllerInfo: androidx.media3.session.MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        job.cancel()
        // No quitar la notificación ni liberar el reproductor si la reproducción
        // está activa: el servicio puede ser recreado y queremos seguir sonando.
        val state = radioPlayer.state.value
        if (state.status != PlaybackStatus.PLAYING && state.status != PlaybackStatus.PAUSED) {
            stopForeground(true)
        }
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun togglePlay() {
        val state = radioPlayer.state.value
        Log.d(TAG, "togglePlay() status=${state.status} station=${state.currentStation?.name}")
        val station = state.currentStation ?: return
        when (state.status) {
            PlaybackStatus.PLAYING -> radioPlayer.pause()
            PlaybackStatus.PAUSED -> radioPlayer.resume()
            else -> radioPlayer.play(station)
        }
    }

    private fun stopPlayback() {
        Log.d(TAG, "stopPlayback()")
        radioPlayer.stop()
        stopForeground(true)
    }

    private fun updateNotification(state: PlayerState) {
        Log.d(TAG, "updateNotification() status=${state.status} station=${state.currentStation?.name}")
        val notification = buildNotification(
            this,
            state.currentStation,
            state.status,
            state.metadataTitle
        )
        notificationManager?.notify(NOTIFICATION_ID, notification)

        // Mantener el servicio en primer plano mientras haya algo cargado
        // (reproduciendo, pausado o cargando). Solo salir de foreground
        // cuando esté IDLE sin estación (reproducción detenida) o ERROR.
        if (state.status == PlaybackStatus.PLAYING || state.status == PlaybackStatus.PAUSED || state.status == PlaybackStatus.LOADING) {
            startForeground(NOTIFICATION_ID, notification)
        } else if (state.currentStation == null) {
            stopForeground(true)
        }
    }
}
