/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2026 jjcampac
 */

/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.jjcampac.eter.diag.diag
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.PlayerState
import com.jjcampac.eter.player.cleanMetadataTitle
import com.jjcampac.eter.player.service.RadioMediaSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del reproductor respaldado por un [MediaController].
 *
 * El unico dueno del ExoPlayer es [RadioMediaSessionService]. La interfaz nunca
 * toca el reproductor: se conecta a la sesion con un [MediaController], le envia
 * los comandos y refleja aqui el estado que publica la sesion.
 *
 * Si el servicio se destruye, [MediaController.Listener.onDisconnected] vuelve a
 * conectar el controlador: la UI no sobrevive a una sesion muerta.
 */
class PlayerViewModel(
    context: Context,
    private val repository: StationRepository
) : ViewModel() {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var isCleared = false

    /** Emisora pendiente mientras el controlador termina de conectar. */
    private var pendingStation: Station? = null

    /** Escucha los cambios de estado que publica la sesion. */
    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            diag(
                "UI: onPlaybackStateChanged state=$playbackState " +
                    "playWhenReady=${controller?.playWhenReady} isPlaying=${controller?.isPlaying}",
                controller
            )
            _state.update { current ->
                val status = when (playbackState) {
                    Player.STATE_BUFFERING -> PlaybackStatus.LOADING
                    Player.STATE_READY ->
                        if (controller?.isPlaying == true) PlaybackStatus.PLAYING
                        else PlaybackStatus.PAUSED
                    Player.STATE_IDLE, Player.STATE_ENDED -> PlaybackStatus.IDLE
                    else -> current.status
                }
                current.copy(status = status, errorMessage = null)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            diag(
                "UI: onIsPlayingChanged isPlaying=$isPlaying status=${_state.value.status}",
                controller
            )
            _state.update { current ->
                current.copy(
                    status =
                    if (isPlaying) PlaybackStatus.PLAYING
                    else if (current.status != PlaybackStatus.IDLE) PlaybackStatus.PAUSED
                    else current.status
                )
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            diag(
                "UI: onPlayWhenReadyChanged playWhenReady=$playWhenReady reason=$reason " +
                    "suppression=${controller?.playbackSuppressionReason}",
                controller
            )
        }

        override fun onPlaybackSuppressionReasonChanged(reason: Int) {
            diag(
                "UI: onPlaybackSuppressionReasonChanged reason=$reason " +
                    "playWhenReady=${controller?.playWhenReady} isPlaying=${controller?.isPlaying}",
                controller
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId ?: return
            diag("UI: onMediaItemTransition mediaId=$id reason=$reason", controller)
            viewModelScope.launch {
                repository.getStation(id)?.let { emisora ->
                    _state.update { it.copy(currentStation = emisora, errorMessage = null) }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            diag("UI: onPlayerError code=${error.errorCode} message=${error.message}", controller)
            val errorMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                    "No hay conexion con la emisora. Comprueba tu red."
                PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                    "El directo avanzo demasiado rapido. Reconectando..."
                else -> "No se puede reproducir esta emisora."
            }
            _state.update {
                it.copy(status = PlaybackStatus.ERROR, errorMessage = errorMessage)
            }
        }

        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val clean = cleanMetadataTitle(metadata.get(i).toString())
                if (clean != null) {
                    _state.update { it.copy(metadataTitle = clean) }
                    return
                }
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val artData = mediaMetadata.artworkData
            val artUri = mediaMetadata.artworkUri
            if (artData != null || artUri != null) {
                _state.update {
                    it.copy(streamArtworkData = artData, streamArtworkUri = artUri)
                }
            }
        }

        override fun onVolumeChanged(volume: Float) {
            _state.update { it.copy(volume = volume) }
        }
    }

    /** Detecta la caida de la sesion y vuelve a conectar el controlador. */
    private val controllerListener = object : MediaController.Listener {

        override fun onDisconnected(controller: MediaController) {
            diag("UI: MediaController.onDisconnected -> reconectando", controller)
            if (isCleared) return
            _state.update { it.copy(status = PlaybackStatus.IDLE) }
            connect()
        }

        override fun onError(controller: MediaController, sessionError: SessionError) {
            diag(
                "UI: MediaController.onError code=${sessionError.code} " +
                    "message=${sessionError.message}",
                controller
            )
            if (isCleared) return
            connect()
        }
    }

    init {
        diag("UI: PlayerViewModel.init (MediaController)")
        connect()
    }

    /** Construye (o reconstruye) el controlador de la sesion. */
    private fun connect() {
        if (isCleared) return
        releaseController()

        val token = SessionToken(
            appContext,
            ComponentName(appContext, RadioMediaSessionService::class.java)
        )
        val future = MediaController.Builder(appContext, token)
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future

        future.addListener(
            {
                if (isCleared) {
                    MediaController.releaseFuture(future)
                    return@addListener
                }
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(playerListener)
                    _state.update {
                        it.copy(
                            status = statusDesde(connected),
                            volume = connected.volume
                        )
                    }
                    pendingStation?.let { emisora ->
                        pendingStation = null
                        playNow(emisora)
                    }
                    diag("UI: MediaController conectado", connected)
                } catch (e: Exception) {
                    diag("UI: MediaController.connect error: ${e.message}")
                    controller = null
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    /** Libera el controlador actual sin marcar el ViewModel como destruido. */
    private fun releaseController() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private fun statusDesde(c: Player): PlaybackStatus = when (c.playbackState) {
        Player.STATE_BUFFERING -> PlaybackStatus.LOADING
        Player.STATE_READY ->
            if (c.isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
        Player.STATE_IDLE, Player.STATE_ENDED -> PlaybackStatus.IDLE
        else -> _state.value.status
    }

    /** Arranca el servicio de medios para que exista una sesion a la que conectar. */
    private fun startService() {
        val intent = Intent(appContext, RadioMediaSessionService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
            diag("UI: startForegroundService solicitado", controller)
        } catch (e: Exception) {
            diag("UI: no se pudo arrancar el servicio: ${e.message}", controller)
        }
    }

    /** Reproduce la emisora o la pausa si ya es la actual y esta sonando. */
    fun togglePlay(station: Station) {
        val current = _state.value
        diag("UI: togglePlay station=${station.name} status=${current.status}", controller)
        if (current.status == PlaybackStatus.PLAYING && current.currentStation?.url == station.url) {
            pause()
        } else if (current.status == PlaybackStatus.PAUSED && current.currentStation?.url == station.url) {
            resume()
        } else {
            play(station)
        }
    }

    fun play(station: Station) {
        diag("UI: play station=${station.name}", controller)
        startService()
        if (controller == null) {
            pendingStation = station
            connect()
            return
        }
        playNow(station)
    }

    private fun playNow(station: Station) {
        val c = controller ?: return
        c.setMediaItem(construirItem(station))
        c.prepare()
        c.playWhenReady = true
        c.play()
    }

    private fun construirItem(station: Station): MediaItem =
        MediaItem.Builder()
            .setUri(station.url)
            .setMediaId(station.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.subtitle.takeIf { it.isNotBlank() })
                    .setArtworkUri(
                        station.imageUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    )
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()

    fun pause() {
        diag("UI: pause", controller)
        controller?.pause()
    }

    fun resume() {
        diag("UI: resume", controller)
        controller?.play()
    }

    fun stop() {
        diag("UI: stop", controller)
        controller?.stop()
    }

    /** Reproduce la siguiente favorita (circular) mediante comando de sesion. */
    fun nextFavorite() {
        diag("UI: nextFavorite", controller)
        controller?.sendCustomCommand(
            SessionCommand(ACTION_FAVORITA_SIGUIENTE, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    /** Reproduce la favorita anterior (circular) mediante comando de sesion. */
    fun prevFavorite() {
        diag("UI: prevFavorite", controller)
        controller?.sendCustomCommand(
            SessionCommand(ACTION_FAVORITA_ANTERIOR, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    override fun onCleared() {
        isCleared = true
        releaseController()
        super.onCleared()
    }

    companion object {
        /** Acciones de sesión compartidas con [RadioMediaSessionService]. */
        private const val ACTION_FAVORITA_ANTERIOR =
            "com.jjcampac.eter.ACTION_FAVORITA_ANTERIOR"
        private const val ACTION_FAVORITA_SIGUIENTE =
            "com.jjcampac.eter.ACTION_FAVORITA_SIGUIENTE"

        /** Fabrica simple (sin Hilt) a partir del contexto y el repositorio. */
        fun factory(context: Context, repository: StationRepository) = viewModelFactory {
            initializer { PlayerViewModel(context, repository) }
        }
    }
}
