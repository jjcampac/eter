package com.jjcampac.eter.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jjcampac.eter.domain.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Estados de reproducción expuestos a la UI. */
enum class PlaybackStatus { IDLE, LOADING, PLAYING, PAUSED, ERROR }

/**
 * Estado observable del reproductor.
 *
 * @param currentStation Emisora cargada en el reproductor.
 * @param errorMessage   Mensaje de error en español (nulo si no hay error).
 * @param metadataTitle  Título de canción recibido vía metadatos ICY (nulo si no hay).
 */
data class PlayerState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentStation: Station? = null,
    val errorMessage: String? = null,
    val metadataTitle: String? = null
)

/**
 * Wrapper ligero sobre [ExoPlayer] (Media3) dedicado a streams de radio.
 *
 * Expone un [StateFlow] con el estado de reproducción y gestiona el ciclo
 * de vida del reproductor. Para streams en directo no tiene sentido una barra
 * de progreso, por eso solo se modelan estados cualitativos.
 */
class RadioPlayer(context: Context) {

    private val appContext = context.applicationContext

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update { current ->
                    val newStatus = when (playbackState) {
                        Player.STATE_BUFFERING -> PlaybackStatus.LOADING
                        Player.STATE_READY ->
                            if (exoPlayer.playWhenReady) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
                        Player.STATE_IDLE, Player.STATE_ENDED -> PlaybackStatus.IDLE
                        else -> current.status
                    }
                    current.copy(status = newStatus, errorMessage = null)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { current ->
                    current.copy(
                        status =
                        if (isPlaying) PlaybackStatus.PLAYING
                        else if (current.status != PlaybackStatus.IDLE) PlaybackStatus.PAUSED
                        else current.status
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                                "No hay conexión con la emisora. Comprueba tu red."
                            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                                "El directo avanzó demasiado rápido. Reconectando…"
                            else -> "No se puede reproducir esta emisora."
                        }
                    )
                }
            }

            // Reconexión automática ante cortes de red y estado "detenido" inesperado.
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_IDLE && exoPlayer.mediaItemCount > 0 && playWhenReady) {
                    exoPlayer.prepare()
                }
            }

            // Metadatos ICY (título de la canción).
            override fun onMetadata(metadata: Metadata) {
                var title: String? = null
                for (i in 0 until metadata.length()) {
                    val str = metadata.get(i).toString()
                    if (str.isNotEmpty()) {
                        title = str
                        break
                    }
                }
                if (title != null && title.isNotEmpty()) {
                    _state.update { it.copy(metadataTitle = title) }
                }
            }
        })
    }

    /** Carga una emisora y comienza la reproducción. */
    fun play(station: Station) {
        _state.value = PlayerState(status = PlaybackStatus.LOADING, currentStation = station)
        exoPlayer.setMediaItem(MediaItem.fromUri(station.url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    /** Pausa la reproducción actual (deja el stream cargado). */
    fun pause() {
        exoPlayer.pause()
        _state.update { it.copy(status = PlaybackStatus.PAUSED, errorMessage = null) }
    }

    /** Reanuda la emisora actual si estaba pausada. */
    fun resume() {
        exoPlayer.playWhenReady = true
        _state.update { it.copy(status = PlaybackStatus.LOADING, errorMessage = null) }
    }

    /** Detiene y libera la emisora actual. */
    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _state.value = PlayerState()
    }

    /** Libera los recursos del reproductor. No debe usarse después de llamarlo. */
    fun release() {
        exoPlayer.release()
    }
}
