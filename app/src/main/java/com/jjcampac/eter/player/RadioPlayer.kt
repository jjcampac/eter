/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.player

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jjcampac.eter.EterApplication
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.util.ReconnectPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "RadioPlayer"

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

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        // Audio focus: al pedir el foco, el sistema pausa o atenua cualquier
        // otro medio que se este reproduciendo (Spotify, YouTube, etc.).
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        setAudioAttributes(audioAttributes, true)
    }

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    // --- Reconexión automática controlada ---
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reconnectJob: Job? = null
    private var reconnectWindowStartMs = 0L

    /** Intervalo entre reintentos de conexión. */
    private val RECONNECT_INTERVAL_MS = 5_000L

    init {
        exoPlayer.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged() state=$playbackState playWhenReady=${exoPlayer.playWhenReady}")
                if (playbackState == Player.STATE_READY) {
                    // Conexión recuperada: se cierra la ventana de reintentos.
                    resetReconnect()
                }
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
                Log.d(TAG, "onIsPlayingChanged() isPlaying=$isPlaying currentStatus=${_state.value.status}")
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
                Log.e(TAG, "onPlayerError() code=${error.errorCode} message=${error.message}")
                val errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                        "No hay conexión con la emisora. Comprueba tu red."
                    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                        "El directo avanzó demasiado rápido. Reconectando…"
                    else -> "No se puede reproducir esta emisora."
                }
                // Reintento controlado: si la ventana configurada se agota, se avisa al usuario.
                startReconnect(errorMessage)
            }

            // Cortes de red inesperados: se recupera al instante solo si NO hay una
            // ventana de reintentos activa (esa ventana la gestiona [startReconnect]).
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_IDLE && exoPlayer.mediaItemCount > 0 &&
                    playWhenReady && reconnectWindowStartMs == 0L
                ) {
                    _state.update { it.copy(status = PlaybackStatus.PLAYING, errorMessage = null) }
                    exoPlayer.prepare()
                }
            }

            // Metadatos ICY (título de la canción).
            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val clean = cleanMetadataTitle(metadata.get(i).toString())
                    if (clean != null) {
                        _state.update { it.copy(metadataTitle = clean) }
                        return
                    }
                }
            }
        })
    }

    /** Carga una emisora y comienza la reproducción. */
    fun play(station: Station) {
        Log.d(TAG, "play() station=${station.name} url=${station.url}")
        // Detener cualquier reproducción previa para evitar conflictos
        stop()
        // Persistir para auto-reanudar tras muerte del proceso (pantalla apagada, etc.)
        (appContext as? EterApplication)?.saveLastStationUrl(station.url)

        _state.value = PlayerState(status = PlaybackStatus.LOADING, currentStation = station)
        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(station.url))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            // Blindaje: una URL inválida o un formato de stream no soportado no debe
            // tumbar la aplicación (ej. HLS antes de incluir media3-exoplayer-hls).
            Log.e(TAG, "play() no se pudo configurar el stream (${station.url}): ${e.message}")
            _state.value = PlayerState(
                status = PlaybackStatus.ERROR,
                currentStation = station,
                errorMessage = "No se puede reproducir esta emisora. Comprueba su URL o su formato."
            )
        }
    }

    /** Pausa la reproducción actual (deja el stream cargado). */
    fun pause() {
        Log.d(TAG, "pause()")
        exoPlayer.pause()
        _state.update { it.copy(status = PlaybackStatus.PAUSED, errorMessage = null) }
    }

    /** Reanuda la emisora actual si estaba pausada. */
    fun resume() {
        Log.d(TAG, "resume()")
        exoPlayer.playWhenReady = true
        _state.update { it.copy(status = PlaybackStatus.PLAYING, errorMessage = null) }
    }

    /** Detiene y libera la emisora actual. */
    fun stop() {
        Log.d(TAG, "stop()")
        resetReconnect()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _state.value = PlayerState()
    }

    /**
     * Inicia (o continúa) la reconexión automática tras un fallo del stream.
     *
     * Reintenta cada [RECONNECT_INTERVAL_MS]. Si la ventana configurada en Ajustes
     * se agota, detiene el reintento y muestra al usuario un mensaje de error
     * claro (caso típico: una emisora que no responde, como esRadio). Si la
     * ventana es "sin límite" (0), reintenta indefinidamente.
     */
    private fun startReconnect(errorMessage: String) {
        reconnectJob?.cancel()
        val windowMs = reconnectWindowMs()
        if (reconnectWindowStartMs == 0L) {
            reconnectWindowStartMs = SystemClock.elapsedRealtime()
        }
        _state.update { it.copy(status = PlaybackStatus.LOADING, errorMessage = null) }
        reconnectJob = reconnectScope.launch {
            while (isActive) {
                delay(RECONNECT_INTERVAL_MS)
                if (!exoPlayer.playWhenReady) {
                    // El usuario ha cortado la reproducción: salir de la ventana.
                    resetReconnect()
                    break
                }
                if (windowMs > 0L) {
                    val elapsed = SystemClock.elapsedRealtime() - reconnectWindowStartMs
                    if (elapsed >= windowMs) {
                        Log.w(TAG, "Reconexión agotada tras $elapsed ms. Avisando al usuario.")
                        resetReconnect()
                        _state.update {
                            it.copy(status = PlaybackStatus.ERROR, errorMessage = errorMessage)
                        }
                        break
                    }
                }
                if (exoPlayer.mediaItemCount > 0) {
                    Log.d(TAG, "Reintentando conexión…")
                    exoPlayer.prepare()
                }
            }
        }
    }

    /** Devuelve la ventana de reintentos en ms (0 = sin límite) según Ajustes. */
    private fun reconnectWindowMs(): Long {
        val seconds = ReconnectPreferences(appContext).getReconnectSeconds()
        return if (seconds <= 0) 0L else seconds * 1000L
    }

    /** Cancela la ventana de reintentos activa (al recuperar, detener o liberar). */
    private fun resetReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectWindowStartMs = 0L
    }

    /** Libera los recursos del reproductor. No debe usarse después de llamarlo. */
    fun release() {
        Log.d(TAG, "release()")
        resetReconnect()
        reconnectScope.cancel()
        exoPlayer.release()
    }

    /**
     * Limpia el título de canción recibido por metadatos ICY.
     *
     * Media3 expone los metadatos como `IcyInfo`, cuyo `toString()` es del tipo
     * `ICY: title="Artista - Canción", url="http://..."`. Icecast, por su parte,
     * envía `StreamTitle='Artista - Canción'`. Para mostrarlo de forma limpia
     * en el mini reproductor y en la notificación se extrae el valor del campo
     * `title` (o `StreamTitle`), descartando marcas (ICY:), la url y el resto de
     * metaetiquetas. Además se descartan marcos de metadatos no legibles como
     * canción (p. ej. `PRIV` que envían los streams HLS) para no mostrar basura.
     * Si no se encuentra ninguna etiqueta razonable, devuelve `null`.
     */
    private fun cleanMetadataTitle(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()

        // 1) Título clásico de Shoutcast/Icecast (`StreamTitle='...'` o `title="..."`).
        val icyMatch = Regex(
            "(?:StreamTitle|title)\\s*=\\s*['\"]([^'\"]*)['\"]",
            RegexOption.IGNORE_CASE
        ).find(trimmed)
        if (icyMatch != null) {
            return icyMatch.groupValues[1].trim().takeIf { it.isNotEmpty() }
        }

        // 2) Etiqueta ID3 de título (TIT2), habitual en algunos streams.
        val tit2 = Regex("(?i)^TIT2\\s*:\\s*(.+)$").find(trimmed)
        if (tit2 != null) {
            return tit2.groupValues[1].trim().trim('\'').trim('"').takeIf { it.isNotEmpty() }
        }

        // 3) Marcos de metadatos no legibles como canción (PRIV, TXXX, TALB, COMM,
        //    APIC, TCON, TDRC, UFID...): se descartan para no mostrar basura.
        if (Regex("^[A-Z][A-Z0-9]{3}\\s*:", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)) {
            return null
        }

        // 4) Fallback: cadena original recortada (quitando comillas y restos conocidos).
        return trimmed
            .removePrefix("ICY:")
            .trim()
            .substringBefore("url = ")
            .substringBefore("url=")
            .substringBefore("StreamUrl = ")
            .substringBefore("StreamUrl=")
            .trim()
            .trim('\'')
            .trim('"')
            .takeIf { it.isNotEmpty() }
    }
}
