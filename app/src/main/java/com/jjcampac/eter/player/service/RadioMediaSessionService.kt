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
package com.jjcampac.eter.player.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.jjcampac.eter.EterApplication
import com.jjcampac.eter.MainActivity
import com.jjcampac.eter.R
import com.jjcampac.eter.diag.diag
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.player.RadioPlayer
import com.jjcampac.eter.player.SleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "RadioMediaSessionService"

/**
 * Servicio de sesión de medios que permite la reproducción en segundo plano.
 *
 * La notificación es la **tarjeta multimedia del sistema** (MediaStyle) que se
 * despliega al bajar desde el borde superior de la pantalla. La construye
 * Media3 a partir de la [MediaSession] y del elemento en reproducción, así que
 * muestra:
 *
 * - la carátula (logo de la emisora) como fondo de la tarjeta,
 * - el nombre de la emisora como título y la canción (metadatos ICY) o el
 *   estado ("Conectando…", "Error de conexión") como texto secundario,
 * - el botón grande de reproducir/pausar,
 * - anterior/siguiente entre favoritas y la estrella de favorita
 *   (comandos propios de sesión),
 * - el selector de salida de audio del sistema ("Este teléfono").
 *
 * El [RadioPlayer] se obtiene de [EterApplication] para sobrevivir a la
 * recreación de la actividad. Las constantes de canal e identificador viven en
 * [RadioNotifications] (mismo paquete).
 *
 * Al ser un [MediaLibraryService] la app también es visible en **Android Auto**
 * (declarada como app de audio en `res/xml/automotive_app_desc.xml`): el sistema
 * pide la biblioteca (raíz → "Favoritas" / "Todas" → emisoras) y reproduce la
 * emisora elegida con la misma sesión. No hay pantallas propias en el coche.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class RadioMediaSessionService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var radioPlayer: RadioPlayer
    private lateinit var sleepTimer: SleepTimer
    private lateinit var repository: StationRepository
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)

    /** ID de la emisora en curso (nulo si no hay ninguna). */
    private var idEmisoraActual: String? = null

    /** `true` si la emisora en curso ya está en favoritas. */
    private var esFavorita = false

    /** `true` si hay una favorita anterior/siguiente a la emisora en curso. */
    private var puedeAnterior = false
    private var puedeSiguiente = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        val app = application as EterApplication
        // El servicio es el dueño único del reproductor: nadie más lo crea.
        radioPlayer = RadioPlayer(app)
        sleepTimer = app.getSleepTimer()
        repository = app.getStationRepository()
        diag("onCreate() inicio", this, radioPlayer.exoPlayer)

        // Notificación multimedia del sistema (MediaStyle) construida por Media3.
        setMediaNotificationProvider(crearProveedorNotificacion())

        // Listener de Media3: avisa si el sistema impide iniciar en primer plano.
        setListener(object : MediaSessionService.Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                diag(
                    "onForegroundServiceStartNotAllowedException (no se pudo iniciar en primer plano)",
                    this@RadioMediaSessionService, mediaSession, radioPlayer.exoPlayer
                )
            }
        })

        val sesion = MediaLibrarySession.Builder(this, radioPlayer.exoPlayer, SesionCallback())
            .setSessionActivity(intentActividad())
            .setMediaButtonPreferences(botonesMedia())
            .build()
        mediaSession = sesion
        addSession(sesion)
        diag("onCreate() sesión creada y añadida", this, sesion, radioPlayer.exoPlayer)

        // Botones de la notificación: se recalculan al cambiar de emisora o al
        // modificar las favoritas (desde la app o desde la propia notificación,
        // ya que Room emite el cambio). Así las flechas solo aparecen cuando de
        // verdad hay una emisora anterior o siguiente en la lista.
        scope.launch {
            combine(
                radioPlayer.state.map { it.currentStation?.id }.distinctUntilChanged(),
                repository.getFavorites()
            ) { idEmisora, favoritas -> idEmisora to favoritas }
                .collect { (idEmisora, favoritas) -> actualizarBotones(idEmisora, favoritas) }
        }

        // Atenuación del volumen del temporizador para dormir: el volumen fluctúa
        // (1.0 al inicio → 0.0 al final) y se aplica en tiempo real al ExoPlayer,
        // también con la pantalla apagada mientras la notificación está activa.
        scope.launch {
            sleepTimer.volume.collect { vol ->
                Log.d(TAG, "volumen del temporizador: $vol")
                radioPlayer.setVolume(vol)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand() action=${intent?.action}")
        diag(
            "onStartCommand action=${intent?.action} flags=$flags startId=$startId " +
                "isPlaybackOngoing=${isPlaybackOngoing()} " +
                "playWhenReady=${radioPlayer.exoPlayer.playWhenReady} " +
                "state=${radioPlayer.exoPlayer.playbackState} " +
                "items=${radioPlayer.exoPlayer.mediaItemCount}",
            this, mediaSession, radioPlayer.exoPlayer
        )
        // Media3 atiende aquí los botones de la notificación (play/pausa,
        // anterior/siguiente, estrella y el descarte de la tarjeta).
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_SLEEP_TIMER) {
            Log.d(TAG, "ACTION_SLEEP_TIMER: deteniendo la reproducción y quitando la notificación.")
            diag(
                "ACTION_SLEEP_TIMER → radioPlayer.stop() + stopForeground + stopSelf()",
                this, mediaSession, radioPlayer.exoPlayer
            )
            radioPlayer.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        diag(
            "onGetSession controller=${controllerInfo.packageName}",
            this, mediaSession, radioPlayer.exoPlayer
        )
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        diag(
            "onTaskRemoved rootIntent=$rootIntent isPlaybackOngoing=${isPlaybackOngoing()} " +
                "isPlaying=${radioPlayer.exoPlayer.isPlaying} " +
                "playWhenReady=${radioPlayer.exoPlayer.playWhenReady} " +
                "state=${radioPlayer.exoPlayer.playbackState} " +
                "items=${radioPlayer.exoPlayer.mediaItemCount}",
            this, mediaSession, radioPlayer.exoPlayer
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean
    ) {
        diag(
            "onUpdateNotification startInForegroundRequired=$startInForegroundRequired " +
                "isPlaybackOngoing=${isPlaybackOngoing()} " +
                "playWhenReady=${radioPlayer.exoPlayer.playWhenReady} " +
                "state=${radioPlayer.exoPlayer.playbackState} " +
                "items=${radioPlayer.exoPlayer.mediaItemCount}",
            this, session, radioPlayer.exoPlayer
        )
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        diag(
            "onDestroy() inicio isPlaybackOngoing=${isPlaybackOngoing()} " +
                "isPlaying=${radioPlayer.exoPlayer.isPlaying} " +
                "playWhenReady=${radioPlayer.exoPlayer.playWhenReady} " +
                "state=${radioPlayer.exoPlayer.playbackState} " +
                "items=${radioPlayer.exoPlayer.mediaItemCount}",
            this, mediaSession, radioPlayer.exoPlayer
        )
        job.cancel()
        // Quitar la sesión del servicio retira también la notificación asociada
        // antes de liberar la sesión (evita tarjetas huérfanas en la cortina).
        mediaSession?.let { sesion ->
            diag("onDestroy() removeSession + release de la sesión", this, sesion, radioPlayer.exoPlayer)
            removeSession(sesion)
            sesion.release()
        }
        mediaSession = null
        // El dueño del reproductor lo libera aquí: si el servicio muere, no queda
        // un ExoPlayer huérfano reproduciendo sin sesión ni primer plano.
        radioPlayer.release()
        scope.cancel()
        super.onDestroy()
        diag("onDestroy() fin", this, radioPlayer)
    }

    /**
     * Proveedor de la notificación multimedia del sistema: canal propio,
     * identificador fijo e icono de la app (el del launcher) en la esquina de
     * la tarjeta. El estilo multimedia lo pone Media3 para que la tarjeta tenga
     * el aspecto nativo de Android.
     */
    private fun crearProveedorNotificacion(): DefaultMediaNotificationProvider =
        DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(RADIO_CHANNEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .setNotificationId(RADIO_NOTIFICATION_ID)
            .build()
            .apply {
                // Icono de la esquina superior izquierda de la tarjeta y de la
                // barra de estado: versión monócroma a 24 dp del logo de la app
                // (el icono del launcher es adaptativo y a ese tamaño se perdía).
                setSmallIcon(R.drawable.ic_notification_eter)
            }

    /** Intent que abre la app al tocar la tarjeta de la notificación. */
    private fun intentActividad(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Botones que se muestran en la notificación:
     *
     * - **Anterior / siguiente**: solo si hay otra favorita antes o después de
     *   la emisora en curso (al principio o al final de la lista no aparecen).
     * - **Estrella**: solo si la emisora en curso no es favorita todavía; desde
     *   la notificación únicamente se puede añadir, no quitar.
     *
     * El botón de reproducir/pausar lo añade Media3 automáticamente en el centro.
     */
    private fun botonesMedia(): List<CommandButton> {
        val botones = mutableListOf<CommandButton>()
        if (puedeAnterior) {
            botones += CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setSessionCommand(SessionCommand(ACCION_FAVORITA_ANTERIOR, Bundle.EMPTY))
                .setDisplayName(getString(R.string.skip_previous))
                .setSlots(CommandButton.SLOT_BACK)
                .build()
        }
        if (puedeSiguiente) {
            botones += CommandButton.Builder(CommandButton.ICON_NEXT)
                .setSessionCommand(SessionCommand(ACCION_FAVORITA_SIGUIENTE, Bundle.EMPTY))
                .setDisplayName(getString(R.string.skip_next))
                .setSlots(CommandButton.SLOT_FORWARD)
                .build()
        }
        if (idEmisoraActual != null && !esFavorita) {
            botones += CommandButton.Builder(CommandButton.ICON_STAR_UNFILLED)
                .setSessionCommand(SessionCommand(ACCION_ANADIR_FAVORITA, Bundle.EMPTY))
                .setDisplayName(getString(R.string.favorite_add))
                .setSlots(CommandButton.SLOT_OVERFLOW)
                .build()
        }
        return botones
    }

    /**
     * Recalcula los botones de la notificación a partir de la emisora en curso y
     * de la lista de favoritas (en el mismo orden que la app): posición en la
     * lista, si ya es favorita y si existen anterior/siguiente. Solo se
     * actualizan las preferencias de la sesión cuando algo cambia; Media3
     * reconstruye entonces la notificación.
     */
    private fun actualizarBotones(idEmisora: String?, favoritas: List<Station>) {
        val indice = if (idEmisora == null) -1 else favoritas.indexOfFirst { it.id == idEmisora }
        val favorita = idEmisora != null && indice >= 0
        val anterior = idEmisora != null && favoritas.isNotEmpty() && indice != 0
        val siguiente = idEmisora != null && favoritas.isNotEmpty() && indice != favoritas.lastIndex

        if (idEmisora == idEmisoraActual && favorita == esFavorita &&
            anterior == puedeAnterior && siguiente == puedeSiguiente
        ) {
            return
        }

        idEmisoraActual = idEmisora
        esFavorita = favorita
        puedeAnterior = anterior
        puedeSiguiente = siguiente
        Log.d(TAG, "botones: favorita=$favorita anterior=$anterior siguiente=$siguiente")
        mediaSession?.setMediaButtonPreferences(botonesMedia())
    }

    /**
     * Reproduce la favorita anterior o la siguiente. No da la vuelta a la lista:
     * en los extremos no hace nada (esas flechas no se muestran).
     */
    private fun reproducirFavorita(anterior: Boolean) {
        scope.launch {
            val favoritas = repository.getFavoritesList()
            if (favoritas.isEmpty()) return@launch
            val idActual = radioPlayer.state.value.currentStation?.id
            val indice = favoritas.indexOfFirst { it.id == idActual }
            val destino = when {
                indice < 0 -> if (anterior) favoritas.lastIndex else 0
                anterior -> indice - 1
                else -> indice + 1
            }
            if (destino !in favoritas.indices) {
                Log.d(TAG, "reproducirFavorita(anterior=$anterior): no hay más favoritas.")
                return@launch
            }
            Log.d(TAG, "reproducirFavorita(anterior=$anterior) → ${favoritas[destino].name}")
            radioPlayer.play(favoritas[destino])
        }
    }

    /**
     * Añade la emisora en curso a favoritas desde la notificación. Quitarla se
     * hace desde la app: la notificación solo ofrece añadir.
     */
    private fun anadirFavorita() {
        val emisora = radioPlayer.state.value.currentStation ?: return
        if (esFavorita) {
            Log.d(TAG, "anadirFavorita(): ya es favorita.")
            return
        }
        scope.launch {
            // Las emisoras de Internet no están en Room: se guardan al marcarlas.
            val orden = repository.nextFavoriteSortOrder()
            repository.addStation(emisora.copy(isFavorite = true, sortOrder = orden))
            Log.d(TAG, "añadida a favoritas: ${emisora.name}")
        }
    }

    // --- Biblioteca para Android Auto -----------------------------------------
    //
    // Android Auto pide el árbol de la biblioteca a la MediaLibrarySession:
    //
    //     raíz → "Favoritas" / "Todas" → emisoras
    //
    // Al elegir una emisora, Media3 llama a onAddMediaItems para resolver su
    // mediaId (el id de la emisora) en la URL del stream. La reproducción usa la
    // misma sesión, así que la notificación y los controles del coche funcionan
    // igual que en el móvil.

    /** Elemento raíz de la biblioteca (solo navegable). */
    private fun itemRaiz(): MediaItem = itemCarpeta(ID_RAIZ, getString(R.string.app_name))

    /** Carpeta navegable de la biblioteca. */
    private fun itemCarpeta(id: String, titulo: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(titulo)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                    .build()
            )
            .build()

    /** Emisora como elemento navegable y reproducible de la biblioteca. */
    private fun itemEmisora(station: Station): MediaItem =
        MediaItem.Builder()
            .setMediaId(station.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.subtitle.takeIf { it.isNotBlank() })
                    .setArtworkUri(
                        station.imageUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    )
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .build()
            )
            .build()

    /** Aplica la página que pide el navegador (si pagina; si no, todo). */
    private fun paginaDe(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> =
        if (pageSize <= 0) items else items.drop(page * pageSize).take(pageSize)

    /**
     * Ejecuta un bloque suspend y entrega su [LibraryResult] como futuro, que es
     * lo que espera la sesión de biblioteca. Room no se puede consultar en el
     * hilo principal, de ahí el salto a la corrutina.
     */
    private fun <T> resultadoAsincrono(
        bloque: suspend () -> LibraryResult<T>
    ): ListenableFuture<LibraryResult<T>> {
        val futuro = SettableFuture.create<LibraryResult<T>>()
        scope.launch {
            try {
                futuro.set(bloque())
            } catch (e: Exception) {
                Log.e(TAG, "error en la biblioteca: ${e.message}")
                // Media3 convierte el fallo del futuro en un error para el navegador.
                futuro.setException(e)
            }
        }
        return futuro
    }

    /**
     * Comandos propios de la sesión (los botones de la notificación no son
     * comandos del reproductor, porque la app solo tiene un elemento cargado) y
     * árbol de la biblioteca que consume Android Auto.
     */
    private inner class SesionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            diag(
                "SesionCallback.onConnect controller=${controller.packageName} " +
                    "notification=${session.isMediaNotificationController(controller)} " +
                    "auto=${session.isAutoCompanionController(controller)}",
                this@RadioMediaSessionService, session, radioPlayer.exoPlayer
            )
            // Se parte de los comandos de sesión **y de biblioteca**: sin los
            // segundos, Android Auto no puede navegar por las emisoras.
            val comandos =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon().apply {
                    add(SessionCommand(ACCION_FAVORITA_ANTERIOR, Bundle.EMPTY))
                    add(SessionCommand(ACCION_FAVORITA_SIGUIENTE, Bundle.EMPTY))
                    add(SessionCommand(ACCION_ANADIR_FAVORITA, Bundle.EMPTY))
                }.build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(comandos)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            Log.d(TAG, "onCustomCommand() action=${customCommand.customAction}")
            diag(
                "onCustomCommand action=${customCommand.customAction} " +
                    "controller=${controller.packageName}",
                this@RadioMediaSessionService, session, radioPlayer.exoPlayer
            )
            when (customCommand.customAction) {
                ACCION_FAVORITA_ANTERIOR -> reproducirFavorita(anterior = true)
                ACCION_FAVORITA_SIGUIENTE -> reproducirFavorita(anterior = false)
                ACCION_ANADIR_FAVORITA -> anadirFavorita()
                else -> return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
                )
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            diag(
                "SesionCallback.onDisconnected controller=${controller.packageName} " +
                    "notification=${session.isMediaNotificationController(controller)}",
                this@RadioMediaSessionService, session, radioPlayer.exoPlayer
            )
            super.onDisconnected(session, controller)
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: Int
        ): Int {
            diag(
                "onPlayerCommandRequest controller=${controller.packageName} command=$command",
                this@RadioMediaSessionService, session, radioPlayer.exoPlayer
            )
            return super.onPlayerCommandRequest(session, controller, command)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(itemRaiz(), params))

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> = resultadoAsincrono {
            when (mediaId) {
                ID_RAIZ -> LibraryResult.ofItem(itemRaiz(), null)
                ID_FAVORITAS -> LibraryResult.ofItem(
                    itemCarpeta(ID_FAVORITAS, getString(R.string.favorites)), null
                )
                ID_TODAS -> LibraryResult.ofItem(
                    itemCarpeta(ID_TODAS, getString(R.string.all_stations)), null
                )
                else -> repository.getStation(mediaId)?.let { emisora ->
                    LibraryResult.ofItem(itemEmisora(emisora), null)
                } ?: LibraryResult.ofError<MediaItem>(LibraryResult.RESULT_ERROR_UNKNOWN)
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = resultadoAsincrono {
            val items = when (parentId) {
                ID_RAIZ -> listOf(
                    itemCarpeta(ID_FAVORITAS, getString(R.string.favorites)),
                    itemCarpeta(ID_TODAS, getString(R.string.all_stations))
                )
                ID_FAVORITAS -> repository.getFavoritesList().map { itemEmisora(it) }
                ID_TODAS -> repository.getAllStations().first().map { itemEmisora(it) }
                else -> emptyList()
            }
            Log.d(TAG, "onGetChildren() $parentId → ${items.size} elementos")
            LibraryResult.ofItemList(ImmutableList.copyOf(paginaDe(items, page, pageSize)), params)
        }

        /**
         * Resuelve los elementos que llegan desde Android Auto (solo llevan el
         * mediaId de la emisora) a elementos reproducibles con su URL y metadatos.
         */
        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val futuro = SettableFuture.create<MutableList<MediaItem>>()
            diag(
                "onAddMediaItems count=${mediaItems.size} controller=${controller.packageName}",
                this@RadioMediaSessionService, session, radioPlayer.exoPlayer
            )
            scope.launch {
                val resueltos = mediaItems.mapNotNull { item ->
                    if (item.localConfiguration != null) {
                        item
                    } else {
                        val emisora = repository.getStation(item.mediaId)
                        if (emisora == null) {
                            Log.w(TAG, "onAddMediaItems(): emisora desconocida ${item.mediaId}")
                            null
                        } else {
                            Log.d(TAG, "onAddMediaItems(): ${emisora.name}")
                            radioPlayer.itemPara(emisora)
                        }
                    }
                }.toMutableList()
                futuro.set(resueltos)
            }
            return futuro
        }
    }

    internal companion object {
        /** Anterior favorita. */
        const val ACCION_FAVORITA_ANTERIOR = "com.jjcampac.eter.ACTION_FAVORITA_ANTERIOR"

        /** Siguiente favorita. */
        const val ACCION_FAVORITA_SIGUIENTE = "com.jjcampac.eter.ACTION_FAVORITA_SIGUIENTE"

        /** Añadir la emisora en curso a favoritas (la notificación no permite quitarla). */
        const val ACCION_ANADIR_FAVORITA = "com.jjcampac.eter.ACTION_ANADIR_FAVORITA"

        /** Id del elemento raíz de la biblioteca de Android Auto. */
        const val ID_RAIZ = "eter:raiz"

        /** Id de la carpeta de favoritas. */
        const val ID_FAVORITAS = "eter:favoritas"

        /** Id de la carpeta con todas las emisoras. */
        const val ID_TODAS = "eter:todas"
    }
}
