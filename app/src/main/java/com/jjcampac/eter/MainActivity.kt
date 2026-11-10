/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.di.AppModule
import com.jjcampac.eter.player.service.RadioMediaSessionService
import com.jjcampac.eter.ui.components.MiniPlayer
import com.jjcampac.eter.ui.main.FavoritesScreen
import com.jjcampac.eter.ui.main.MainScreen
import com.jjcampac.eter.ui.main.MainViewModel
import com.jjcampac.eter.ui.main.SearchSource
import com.jjcampac.eter.ui.player.PlayerScreen
import com.jjcampac.eter.ui.player.PlayerViewModel
import com.jjcampac.eter.ui.settings.SettingsScreen
import com.jjcampac.eter.ui.station.StationFormScreen
import com.jjcampac.eter.ui.station.StationFormViewModel
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.theme.EterShapePanel
import com.jjcampac.eter.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import com.jjcampac.eter.ui.theme.ThemeViewModel
import com.jjcampac.eter.util.AppLanguage

private const val TAG = "MainActivity"

/**
 * Actividad principal de Eter.
 *
 * Estructura visual: Scaffold con barra superior, mini reproductor y barra de
 * navegación inferior con tres pestañas (Favoritas, Estaciones, Ajustes).
 * El reproductor a pantalla completa se muestra en lugar del Scaffold cuando
 * el usuario pulsa una emisora; al volver, el mini reproductor sigue activo.
 *
 * La reproducción se delega en [RadioMediaSessionService] (servicio en primer
 * plano) para que continúe con la pantalla apagada o desde la notificación.
 */
class MainActivity : ComponentActivity() {

    /** Aplica el idioma guardado en Ajustes envolviendo el contexto base. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrapWithAppLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() savedInstanceState=${savedInstanceState != null}")
        enableEdgeToEdge()
        setContent {
            RootContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RootContent() {
        val radioPlayer = remember { AppModule.radioPlayer(applicationContext) }
        val stationRepository = remember { AppModule.stationRepository(applicationContext) }
        val sleepTimer = remember { AppModule.sleepTimer(applicationContext) }

        val mainViewModel: MainViewModel = viewModel(
            factory = MainViewModel.factory(
                stationRepository,
                AppLanguage.defaultCountryDisplayName(AppLanguage.current(applicationContext))
            )
        )
        val playerViewModel: PlayerViewModel =
            viewModel(factory = PlayerViewModel.factory(radioPlayer, stationRepository))

        // Pantalla actual: `null` = pestañas; id = reproductor de esa emisora.
        var currentStationId by rememberSaveable { mutableStateOf<String?>(null) }
        var showForm by rememberSaveable { mutableStateOf(false) }
        var editingStationId by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedTab by rememberSaveable { mutableStateOf(0) }
        // Formulario de emisora: el ViewModel se crea una sola vez y sobrevive al
        // cierre del formulario (lo mantiene el ViewModelStore de la Activity).
        // Por eso cada apertura llama a `open(...)`, que reinicia su estado: así el
        // alta aparece siempre en blanco y la edición carga su propia emisora.
        val formViewModel: StationFormViewModel = viewModel(
            factory = StationFormViewModel.factory(stationRepository, editingStationId)
        )
        val stations by stationRepository.getAllStations()
            .collectAsStateWithLifecycle(initialValue = emptyList())
        // Estado de la pantalla principal: permite localizar emisoras de Internet
        // (Radio Browser) que aún no están guardadas en Room.
        val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
        // Favoritas guardadas: habilitan los saltos anterior/siguiente del reproductor.
        val favorites by mainViewModel.favorites.collectAsStateWithLifecycle()
        val currentStation = remember(currentStationId, stations, mainUiState.internetStations) {
            stations.firstOrNull { it.id == currentStationId }
                ?: mainUiState.internetStations.firstOrNull { it.id == currentStationId }
        }
        val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()

        // Si el reproductor cambia de emisora (saltos anterior/siguiente o controles
        // del sistema) mientras el detalle está abierto, se actualiza el id para que
        // la pantalla siga mostrando la emisora que realmente suena.
        LaunchedEffect(playerState.currentStation?.id) {
            val playingId = playerState.currentStation?.id
            if (currentStationId != null && playingId != null && currentStationId != playingId) {
                currentStationId = playingId
            }
        }

        // Mostrar aviso de error de conexión en cualquier pantalla (Fase 5).
        // Se muestra una sola vez por mensaje de error concreto para no saturar.
        var errorToastMessage by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(playerState.status, playerState.errorMessage) {
            if (playerState.errorMessage != null && errorToastMessage != playerState.errorMessage) {
                errorToastMessage = playerState.errorMessage
            }
        }
        // Tema de la app (guardado en SharedPreferences). Se observa el flujo para
        // que `EterTheme` se recomponga en vivo al cambiarlo desde Ajustes.
        val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(applicationContext))
        val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

        EterTheme(themeMode = themeMode) {
        Box(Modifier.fillMaxSize()) {

        // Iniciar el servicio de medios según haya estación activa.
        if (currentStationId != null) {
            startRadioService()
        }
        if (currentStation == null && currentStationId != null) {
            currentStationId = null
        }

        // Al pulsar el minireproductor se abre la pantalla completa.
        val openPlayerDetail: (com.jjcampac.eter.domain.model.Station) -> Unit = { station ->
            currentStationId = station.id
            startRadioService()
        }

        // Al pulsar una tarjeta se reproduce directamente (sin abrir detalle).
        val playStation: (com.jjcampac.eter.domain.model.Station) -> Unit = { station ->
            playerViewModel.play(station)
            startRadioService()
        }
        val editStation: (com.jjcampac.eter.domain.model.Station) -> Unit = { station ->
            editingStationId = station.id
            // Reinicia el formulario con la emisora a editar antes de mostrarlo.
            formViewModel.open(station.id)
            showForm = true
        }

        if (showForm) {
            BackHandler(onBack = {
                showForm = false
                editingStationId = null
            })
            StationFormScreen(
                viewModel = formViewModel,
                onBack = {
                    showForm = false
                    editingStationId = null
                },
                onSaved = {
                    showForm = false
                    editingStationId = null
                }
            )
        } else if (currentStation == null) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            val titleRes = when (selectedTab) {
                                0 -> R.string.favorites_title
                                1 -> R.string.all_stations
                                else -> R.string.tab_settings
                            }
                            Column {
                                // Nombre de la app sobre el título de cada pantalla.
                                Text(
                                    text = stringResource(R.string.app_name).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(stringResource(titleRes))
                            }
                        }
                    )
                },
                bottomBar = {
                    Column {
                        // Mini reproductor (solo visible si hay emisora cargada).
                        MiniPlayer(
                            playerState = playerState,
                            sleepTimer = sleepTimer,
                            onClick = {
                                playerState.currentStation?.let { station ->
                                    openPlayerDetail(station)
                                }
                            },
                            onPlayPause = {
                                playerState.currentStation?.let { station ->
                                    playerViewModel.togglePlay(station)
                                }
                            }
                        )
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_favorites)) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Filled.Radio, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_stations)) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_settings)) }
                            )
                        }
                    }
                },
                floatingActionButton = {
                    // FAB "Añadir emisora": solo visible en la pestaña de
                    // Estaciones y cuando el origen seleccionado es "Mis emisoras".
                    if (selectedTab == 1 && mainUiState.source == SearchSource.LOCAL) {
                        FloatingActionButton(
                            onClick = {
                                editingStationId = null
                                // Formulario en blanco en cada pulsación del FAB.
                                formViewModel.open(null)
                                showForm = true
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (selectedTab) {
                        0 -> FavoritesScreen(
                            mainViewModel = mainViewModel,
                            playerViewModel = playerViewModel,
                            onStationClick = playStation,
                            onToggleFavorite = { station -> mainViewModel.toggleFavorite(station) }
                        )
                        1 -> MainScreen(
                            mainViewModel = mainViewModel,
                            playerViewModel = playerViewModel,
                            onStationClick = playStation,
                            onEditStation = editStation,
                            onDeleteStation = { station -> mainViewModel.deleteStation(station) },
                            onToggleFavorite = { station -> mainViewModel.toggleFavorite(station) }
                        )
                        else -> SettingsScreen()
                    }
                }
            }
        } else {
            val station = currentStation
            // Posición de la emisora que suena dentro de las favoritas: permite
            // deshabilitar "anterior" en la primera y "siguiente" en la última.
            val currentFavoriteIndex =
                favorites.indexOfFirst { it.url == playerState.currentStation?.url }
            BackHandler(onBack = { currentStationId = null })
            PlayerScreen(
                station = station,
                playerViewModel = playerViewModel,
                sleepTimer = sleepTimer,
                onBack = {
                    // Volver a las pestañas SIN detener el servicio de radio:
                    // la reproducción se gestiona desde la notificación y el
                    // mini reproductor. ExoPlayer comparte el proceso y Android
                    // lo mataría a los ~30s si dejáramos de estar en primer plano.
                    currentStationId = null
                },
                onToggleFavorite = { favoriteStation ->
                    mainViewModel.toggleFavorite(favoriteStation)
                },
                canSkipPrevious = favorites.isNotEmpty() && currentFavoriteIndex != 0,
                canSkipNext = favorites.isNotEmpty() &&
                    currentFavoriteIndex != favorites.lastIndex
            )
        }

        // Overlay de aviso de error de conexión (Fase 5): se muestra sobre
        // cualquier pantalla con los colores del tema seleccionado.
        ErrorToast(message = errorToastMessage, onDismiss = { errorToastMessage = null })
    }
}
}

    /**
     * Arranca el servicio de medios.
     *
     * Solo se usa `startForegroundService` cuando hay reproducción en marcha
     * (Media3 mantiene el servicio en primer plano con la notificación
     * multimedia). En el resto de casos basta `startService`, que aquí siempre
     * se llama con la interfaz en primer plano: así se evita el contrato de los
     * 5 segundos de `startForegroundService` cuando aún no suena nada.
     */
    private fun startRadioService() {
        val intent = Intent(this, RadioMediaSessionService::class.java)
        val reproduciendo = (application as EterApplication).getRadioPlayer().exoPlayer.playWhenReady
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && reproduciendo) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar el servicio de radio: ${e.message}")
        }
    }

    private fun stopRadioService() {
        val intent = Intent(this, RadioMediaSessionService::class.java)
        stopService(intent)
    }

    /**
     * Aviso de error de conexión con los mismos colores y forma del mini reproductor.
     * Se muestra como un overlay compacto en la parte inferior de la pantalla, sobre cualquier
     * pestaña (Favoritas, Estaciones, Ajustes o reproductor).
     *
     * Se auto-oculta tras 4 segundos o al pulsar el botón de aceptar.
     */
    @Composable
    private fun ErrorToast(message: String?, onDismiss: () -> Unit) {
        if (message.isNullOrEmpty()) return

        // Auto-dismiss after 4 seconds, like a standard Toast.
        LaunchedEffect(message) {
            delay(4000)
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .wrapContentWidth(),
                shape = EterShapePanel,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
