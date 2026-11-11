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
package com.jjcampac.eter.player

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jjcampac.eter.EterApplication
import com.jjcampac.eter.R
import com.jjcampac.eter.diag.diag
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "RadioPlayer"

/** Estados de reproducción expuestos a la UI. */
enum class PlaybackStatus { IDLE, LOADING, PLAYING, PAUSED, ERROR }

/**
 * Estado observable del reproductor.
 *
 * @param currentStation   Emisora cargada en el reproductor.
 * @param errorMessage     Mensaje de error en español (nulo si no hay error).
 * @param metadataTitle    Título de canción recibido vía metadatos ICY (nulo si no hay).
 * @param streamArtworkData Carátula incrustada en el stream (ID3 APIC, etc.) como bytes,
 *                          recibida de `MediaMetadata.artworkData` de Media3.
 * @param streamArtworkUri  Carátula del stream como URI (MediaMetadata.artworkUri), si la
 *                          entrega el contenedor. Poco habitual en directo.
 */
data class PlayerState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentStation: Station? = null,
    val errorMessage: String? = null,
    val metadataTitle: String? = null,
    val volume: Float = 1.0f,
    val streamArtworkData: ByteArray? = null,
    val streamArtworkUri: Uri? = null
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

    /** Flujo de solo lectura del volumen actual (0.0 a 1.0). */
    private val volume = MutableStateFlow(1.0f)
    val volumeFlow: StateFlow<Float> = volume.asStateFlow()

    /** Aplica el volumen actual al ExoPlayer. */
    private fun applyVolume() {
        val vol = volume.value
        exoPlayer.setVolume(vol)
        Log.d(TAG, "applyVolume($vol)")
    }

    /** Establece el volumen del reproductor (0.0 a 1.0). */
    fun setVolume(vol: Float) {
        volume.value = vol.coerceIn(0.0f, 1.0f)
        applyVolume()
    }

    // --- Reconexión automática controlada ---
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reconnectJob: Job? = null
    private var reconnectWindowStartMs = 0L

    // --- Metadatos de la notificación multimedia ---
    /** Ámbito que mantiene al día el texto secundario de la notificación. */
    private val metadataScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Intervalo entre reintentos de conexión. */
    private val RECONNECT_INTERVAL_MS = 5_000L

    init {
        exoPlayer.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged() state=$playbackState playWhenReady=${exoPlayer.playWhenReady}")
                diag(
                    "onPlaybackStateChanged state=$playbackState " +
                        "playWhenReady=${exoPlayer.playWhenReady} isPlaying=${exoPlayer.isPlaying}",
                    exoPlayer
                )
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
                diag(
                    "onIsPlayingChanged isPlaying=$isPlaying status=${_state.value.status} " +
                        "playWhenReady=${exoPlayer.playWhenReady} " +
                        "suppression=${exoPlayer.playbackSuppressionReason}",
                    exoPlayer
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

            /**
             * Cambios de playWhenReady y de supresión (foco de audio): son los
             * eventos que muestran pérdidas/ganancias de foco y pausas del sistema.
             */
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                diag(
                    "onPlayWhenReadyChanged playWhenReady=$playWhenReady reason=$reason " +
                        "state=${exoPlayer.playbackState} suppression=${exoPlayer.playbackSuppressionReason}",
                    exoPlayer
                )
            }

            override fun onPlaybackSuppressionReasonChanged(reason: Int) {
                diag(
                    "onPlaybackSuppressionReasonChanged reason=$reason " +
                        "playWhenReady=${exoPlayer.playWhenReady} isPlaying=${exoPlayer.isPlaying}",
                    exoPlayer
                )
            }

            /**
             * Cambio de elemento desde fuera de la app (Android Auto, asistente de
             * voz, etc.): la sesión de medios carga el elemento directamente en el
             * reproductor, sin pasar por [play]. Aquí se recupera la emisora por su
             * id para que la UI, el mini reproductor y la notificación sigan
             * mostrando la emisora correcta.
             */
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId ?: return
                if (id == _state.value.currentStation?.id) return
                Log.d(TAG, "onMediaItemTransition() externo mediaId=$id reason=$reason")
                diag("onMediaItemTransition mediaId=$id reason=$reason", exoPlayer)
                metadataScope.launch {
                    val app = appContext as? EterApplication ?: return@launch
                    val emisora = app.getStationRepository().getStation(id) ?: return@launch
                    app.saveLastStationUrl(emisora.url)
                    _state.update { current ->
                        current.copy(currentStation = emisora, errorMessage = null)
                    }
                    Log.d(TAG, "emisora cambiada desde fuera: ${emisora.name}")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "onPlayerError() code=${error.errorCode} message=${error.message}")
                diag("onPlayerError code=${error.errorCode} message=${error.message}", exoPlayer)
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
                diag(
                    "onPlayerStateChanged playWhenReady=$playWhenReady state=$playbackState " +
                        "items=${exoPlayer.mediaItemCount}",
                    exoPlayer
                )
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

            // Carátula incrustada en el stream (Media3: MediaMetadata de la pista).
            // Solo se guarda cuando el contenedor entrega imagen (ID3 APIC, etc.).
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val artData = mediaMetadata.artworkData
                val artUri = mediaMetadata.artworkUri
                if (artData != null || artUri != null) {
                    Log.d(
                        TAG,
                        "Carátula de stream recibida data=${artData?.size} bytes uri=$artUri"
                    )
                    _state.update {
                        it.copy(streamArtworkData = artData, streamArtworkUri = artUri)
                    }
                }
            }
        })

        // Aplicar el volumen inicial (1.0) al reproductor.
        applyVolume()

        // Instrumentación temporal: identidad del reproductor único de la app.
        diag("RadioPlayer.init (creado)", this, exoPlayer)

        // Texto secundario de la notificación multimedia: canción (ICY),
        // "Conectando…" o el estado de error. Se actualiza sin reiniciar el
        // directo porque el elemento se sustituye manteniendo la misma URL.
        metadataScope.launch {
            _state
                .map { textoNotificacion(it) }
                .distinctUntilChanged()
                .collect { texto -> aplicarTextoNotificacion(texto) }
        }
    }

    /**
     * Texto secundario que muestra la tarjeta multimedia del sistema: la
     * canción en curso si la emisora envía metadatos ICY y, si no, el estado
     * de reproducción o la etiqueta de la emisora (país · género).
     */
    private fun textoNotificacion(state: PlayerState): String? = when (state.status) {
        PlaybackStatus.ERROR -> appContext.getString(R.string.state_error)
        PlaybackStatus.LOADING -> state.metadataTitle ?: appContext.getString(R.string.buffering)
        else -> state.metadataTitle ?: state.currentStation?.subtitle?.takeIf { it.isNotBlank() }
    }

    /**
     * Construye el elemento reproducible con los metadatos que el sistema usa
     * en la notificación multimedia y en la pantalla de bloqueo: título (nombre
     * de la emisora), texto secundario (canción o estado) y carátula (logo).
     */
    private fun construirItem(station: Station, texto: String?): MediaItem =
        MediaItem.Builder()
            .setUri(station.url)
            .setMediaId(station.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(texto ?: station.subtitle.takeIf { it.isNotBlank() })
                    .setArtworkUri(station.imageUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()

    /**
     * Sustituye el elemento en reproducción por uno equivalente cuyo único
     * cambio son los metadatos, de modo que Media3 reconstruya la notificación
     * sin cortar el directo.
     */
    private fun aplicarTextoNotificacion(texto: String?) {
        val station = _state.value.currentStation ?: return
        if (exoPlayer.mediaItemCount == 0) return
        val actual = exoPlayer.getMediaItemAt(0).mediaMetadata.artist?.toString()
        if (actual == (texto ?: station.subtitle.takeIf { it.isNotBlank() })) return
        Log.d(TAG, "texto de la notificación: $texto")
        exoPlayer.replaceMediaItem(0, construirItem(station, texto))
    }

    /**
     * Construye el elemento reproducible de una emisora con los metadatos que
     * usa la notificación (título, texto secundario y logo). La sesión lo usa
     * también para resolver las emisoras que llegan de Android Auto.
     */
    fun itemPara(station: Station): MediaItem = construirItem(station, texto = null)

    /** Carga una emisora y comienza la reproducción. */
    fun play(station: Station) {
        Log.d(TAG, "play() station=${station.name} url=${station.url}")
        diag("play() station=${station.name} url=${station.url}", this, exoPlayer)
        // Detener cualquier reproducción previa para evitar conflictos
        stop()
        // Persistir para auto-reanudar tras muerte del proceso (pantalla apagada, etc.)
        (appContext as? EterApplication)?.saveLastStationUrl(station.url)

        _state.value = PlayerState(status = PlaybackStatus.LOADING, currentStation = station)
        try {
            // Los metadatos del elemento alimentan la notificación multimedia
            // (título, canción y logo de la emisora como carátula). Mientras
            // conecta se muestra "Conectando…" como texto secundario.
            exoPlayer.setMediaItem(construirItem(station, texto = textoNotificacion(_state.value)))
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
        diag("pause()", this, exoPlayer)
        exoPlayer.pause()
        _state.update { it.copy(status = PlaybackStatus.PAUSED, errorMessage = null) }
    }

    /** Reanuda la emisora actual si estaba pausada. */
    fun resume() {
        Log.d(TAG, "resume()")
        diag("resume()", this, exoPlayer)
        exoPlayer.playWhenReady = true
        _state.update { it.copy(status = PlaybackStatus.PLAYING, errorMessage = null) }
    }

    /** Detiene y libera la emisora actual. */
    fun stop() {
        Log.d(TAG, "stop()")
        diag("stop()", this, exoPlayer)
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
        diag("startReconnect error=$errorMessage", this, exoPlayer)
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
        diag("resetReconnect()", this, exoPlayer)
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectWindowStartMs = 0L
    }

    /** Libera los recursos del reproductor. No debe usarse después de llamarlo. */
    fun release() {
        Log.d(TAG, "release()")
        diag("release() del RadioPlayer", this, exoPlayer)
        resetReconnect()
        reconnectScope.cancel()
        metadataScope.cancel()
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
}

internal fun cleanMetadataTitle(raw: String?): String? {
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
