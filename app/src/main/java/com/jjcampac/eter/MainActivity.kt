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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.di.AppModule
import com.jjcampac.eter.diag.diag
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
import com.jjcampac.eter.ui.theme.ThemeViewModel
import com.jjcampac.eter.util.AppLanguage
import com.jjcampac.eter.ui.components.EterTopAppBar
import com.jjcampac.eter.ui.components.EterNavigationBar
import com.jjcampac.eter.ui.components.ErrorToast

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

    // --- Instrumentación temporal del ciclo de vida de la Activity ---

    override fun onStart() {
        super.onStart()
        diag("MainActivity.onStart", this)
    }

    override fun onStop() {
        super.onStop()
        diag("MainActivity.onStop", this)
    }

    override fun onDestroy() {
        super.onDestroy()
        diag("MainActivity.onDestroy", this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RootContent() {
        val stationRepository = remember { AppModule.stationRepository(applicationContext) }
        val sleepTimer = remember { AppModule.sleepTimer(applicationContext) }

        val mainViewModel: MainViewModel = viewModel(
            factory = MainViewModel.factory(
                stationRepository,
                AppLanguage.defaultCountryDisplayName(AppLanguage.current(applicationContext))
            )
        )
        val playerViewModel: PlayerViewModel =
            viewModel(factory = PlayerViewModel.factory(applicationContext, stationRepository))

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
                    val titleRes = when (selectedTab) {
                        0 -> R.string.favorites_title
                        1 -> R.string.all_stations
                        else -> R.string.tab_settings
                    }
                    EterTopAppBar(title = stringResource(titleRes))
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
                        EterNavigationBar(
                            selectedTab = selectedTab,
                            onTabSelected = { tab -> selectedTab = tab }
                        )
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
        diag("startRadioService", this)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar el servicio de radio: ${e.message}")
            diag("startRadioService ERROR: ${e.message}", this)
        }
    }

    private fun stopRadioService() {
        val intent = Intent(this, RadioMediaSessionService::class.java)
        diag("stopRadioService", this)
        stopService(intent)
    }
}
