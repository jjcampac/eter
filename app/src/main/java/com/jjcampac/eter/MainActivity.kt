package com.jjcampac.eter

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.di.AppModule
import com.jjcampac.eter.player.service.RadioMediaSessionService
import com.jjcampac.eter.ui.components.MiniPlayer
import com.jjcampac.eter.ui.main.FavoritesScreen
import com.jjcampac.eter.ui.main.MainScreen
import com.jjcampac.eter.ui.main.MainViewModel
import com.jjcampac.eter.ui.player.PlayerScreen
import com.jjcampac.eter.ui.player.PlayerViewModel
import com.jjcampac.eter.ui.settings.SettingsScreen
import com.jjcampac.eter.ui.station.StationFormScreen
import com.jjcampac.eter.ui.station.StationFormViewModel
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemeViewModel

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

        val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(stationRepository))
        val playerViewModel: PlayerViewModel =
            viewModel(factory = PlayerViewModel.factory(radioPlayer, stationRepository))

        // Pantalla actual: `null` = pestañas; id = reproductor de esa emisora.
        var currentStationId by rememberSaveable { mutableStateOf<String?>(null) }
        var showForm by rememberSaveable { mutableStateOf(false) }
        var editingStationId by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedTab by rememberSaveable { mutableStateOf(0) }
        val stations by stationRepository.getAllStations()
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val currentStation = remember(currentStationId, stations) {
            stations.firstOrNull { it.id == currentStationId }
        }
        val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()

        // Tema de la app (guardado en SharedPreferences)
        val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(applicationContext))
        val themeMode by remember { mutableStateOf(themeViewModel.themeMode) }

        EterTheme(themeMode = themeMode) {

        // Iniciar el servicio de medios según haya estación activa.
        if (currentStationId != null) {
            startRadioService()
        }
        if (currentStation == null && currentStationId != null) {
            currentStationId = null
        }

        val openStation: (com.jjcampac.eter.domain.model.Station) -> Unit = { station ->
            currentStationId = station.id
            startRadioService()
        }
        val editStation: (com.jjcampac.eter.domain.model.Station) -> Unit = { station ->
            editingStationId = station.id
            showForm = true
        }

        if (showForm) {
            val formViewModel: StationFormViewModel = viewModel(
                factory = StationFormViewModel.factory(stationRepository, editingStationId)
            )
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
                            Text(stringResource(titleRes))
                        }
                    )
                },
                bottomBar = {
                    Column {
                        // Mini reproductor (solo visible si hay emisora cargada).
                        MiniPlayer(
                            playerState = playerState,
                            onClick = {
                                playerState.currentStation?.let { station ->
                                    openStation(station)
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
                    if (selectedTab == 1) {
                        FloatingActionButton(
                            onClick = {
                                editingStationId = null
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
                            onStationClick = openStation,
                            onToggleFavorite = { station -> mainViewModel.toggleFavorite(station) },
                            onEditStation = editStation,
                            onDeleteStation = { station -> mainViewModel.deleteStation(station) }
                        )
                        1 -> MainScreen(
                            mainViewModel = mainViewModel,
                            playerViewModel = playerViewModel,
                            onStationClick = openStation,
                            onEditStation = editStation,
                            onDeleteStation = { station -> mainViewModel.deleteStation(station) },
                            onToggleFavorite = { station -> mainViewModel.toggleFavorite(station) }
                        )
                        else -> SettingsScreen()
                    }
                }
            }
        } else {
            BackHandler(onBack = { currentStationId = null })
            PlayerScreen(
                station = currentStation,
                playerViewModel = playerViewModel,
                onBack = {
                    // Volver a las pestañas SIN detener el servicio de radio:
                    // la reproducción se gestiona desde la notificación y el
                    // mini reproductor. ExoPlayer comparte el proceso y Android
                    // lo mataría a los ~30s si dejáramos de estar en primer plano.
                    currentStationId = null
                }
            )
        }
    }
}

    private fun startRadioService() {
        val intent = Intent(this, RadioMediaSessionService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopRadioService() {
        val intent = Intent(this, RadioMediaSessionService::class.java)
        stopService(intent)
    }
}
