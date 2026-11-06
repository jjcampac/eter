package com.jjcampac.eter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.di.AppModule
import com.jjcampac.eter.ui.main.MainScreen
import com.jjcampac.eter.ui.main.MainViewModel
import com.jjcampac.eter.ui.player.PlayerScreen
import com.jjcampac.eter.ui.player.PlayerViewModel
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.util.TestData

/**
 * Actividad principal de Eter.
 *
 * Muestra la lista de emisoras y el reproductor, con una navegación simple
 * basada en estado (sin librería externa). El reproductor es único y se
 * comparte entre la UI y el servicio de medios (Fase 2).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EterTheme {
                val radioPlayer = remember { AppModule.radioPlayer(applicationContext) }

                val mainViewModel: MainViewModel = viewModel()
                val playerViewModel: PlayerViewModel =
                    viewModel(factory = PlayerViewModel.factory(radioPlayer))

                // Pantalla actual: `null` = lista; id = reproductor de esa emisora.
                var currentStationId by rememberSaveable { mutableStateOf<String?>(null) }
                val currentStation = TestData.all.firstOrNull { it.id == currentStationId }

                if (currentStation == null) {
                    MainScreen(
                        mainViewModel = mainViewModel,
                        playerViewModel = playerViewModel,
                        onStationClick = { station -> currentStationId = station.id }
                    )
                } else {
                    PlayerScreen(
                        station = currentStation,
                        playerViewModel = playerViewModel,
                        onBack = { currentStationId = null }
                    )
                }
            }
        }
    }
}
