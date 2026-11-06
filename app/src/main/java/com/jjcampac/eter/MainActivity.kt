package com.jjcampac.eter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.di.AppModule
import com.jjcampac.eter.player.service.RadioMediaSessionService
import com.jjcampac.eter.ui.main.MainScreen
import com.jjcampac.eter.ui.main.MainViewModel
import com.jjcampac.eter.ui.player.PlayerScreen
import com.jjcampac.eter.ui.player.PlayerViewModel
import com.jjcampac.eter.ui.station.StationFormScreen
import com.jjcampac.eter.ui.station.StationFormViewModel
import com.jjcampac.eter.ui.theme.EterTheme

private const val TAG = "MainActivity"

/**
 * Actividad principal de Eter.
 *
 * Muestra la lista de emisoras y el reproductor, con una navegación simple
 * basada en estado (sin librería externa). El reproductor es único y se
 * comparte entre la UI y el servicio de medios (Fase 2).
 *
 * Cuando el usuario selecciona una emisora, se inicia el servicio en primer
 * plano para que la reproducción continúe con la pantalla apagada.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() savedInstanceState=${savedInstanceState != null}")
        enableEdgeToEdge()
        setContent {
            EterTheme {
                val radioPlayer = remember { AppModule.radioPlayer(applicationContext) }
                val stationRepository = remember { AppModule.stationRepository(applicationContext) }

                val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(stationRepository))
                val playerViewModel: PlayerViewModel =
                    viewModel(factory = PlayerViewModel.factory(radioPlayer, stationRepository))

                // Pantalla actual: `null` = lista; id = reproductor de esa emisora.
                var currentStationId by rememberSaveable { mutableStateOf<String?>(null) }
                var showForm by rememberSaveable { mutableStateOf(false) }
                var editingStationId by rememberSaveable { mutableStateOf<String?>(null) }
                val stations by stationRepository.getAllStations().collectAsStateWithLifecycle(initialValue = emptyList())
                val currentStation = remember(currentStationId, stations) {
                    stations.firstOrNull { it.id == currentStationId }
                }

                // Iniciar/detener el servicio de medios según haya estación activa.
                if (currentStationId != null) {
                    startRadioService()
                }

                if (currentStation == null && currentStationId != null) {
                    currentStationId = null
                }

                if (showForm) {
                    val formViewModel: StationFormViewModel = viewModel(
                        factory = StationFormViewModel.factory(stationRepository, editingStationId)
                    )
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
                    MainScreen(
                        mainViewModel = mainViewModel,
                        playerViewModel = playerViewModel,
                        onStationClick = { station ->
                            currentStationId = station.id
                            startRadioService()
                        },
                        onAddStation = {
                            editingStationId = null
                            showForm = true
                        },
                        onEditStation = { station ->
                            editingStationId = station.id
                            showForm = true
                        },
                        onDeleteStation = { station ->
                            mainViewModel.deleteStation(station)
                        },
                        onToggleFavorite = { station ->
                            mainViewModel.toggleFavorite(station)
                        }
                    )
                } else {
                    PlayerScreen(
                        station = currentStation,
                        playerViewModel = playerViewModel,
                        onBack = {
                            // Volver a la lista SIN detener el servicio de radio:
                            // ExoPlayer comparte el proceso y Android lo mataria a los ~30s
                            // si dejamos de estar en primer plano. La reproduccion se
                            // gestiona desde la notificacion (play/pausa/detener).
                            currentStationId = null
                        }
                    )
                }
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
