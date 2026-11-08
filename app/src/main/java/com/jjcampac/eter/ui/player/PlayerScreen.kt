/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.jjcampac.eter.ui.components.AnimatedWavesIcon
import com.jjcampac.eter.R
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.SleepTimer

private const val TAG = "PlayerScreen"

/**
 * Pantalla del reproductor a pantalla completa.
 *
 * Muestra la emisora seleccionada y permite reproducir/pausar con un botón
 * central. El estado viene del [PlayerViewModel] compartido, así que es
 * coherente con el mini reproductor y con los controles del sistema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    station: Station,
    playerViewModel: PlayerViewModel,
    sleepTimer: SleepTimer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "PlayerScreen() station=${station.name} url=${station.url}")
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val sleepTimerState by sleepTimer.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = station.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedWavesIcon(tint = MaterialTheme.colorScheme.primary)

            // Segunda línea del mini reproductor: el título de la canción solo se
            // muestra mientras la emisora está en reproducción. En pausa, conectando,
            // con error o parado se muestra el estado en lenguaje claro.
            val secondLine = when (playerState.status) {
                PlaybackStatus.PLAYING ->
                    playerState.metadataTitle?.takeIf { it.isNotEmpty() }
                        ?: stringResource(R.string.radio_live)
                PlaybackStatus.PAUSED -> stringResource(R.string.radio_paused)
                PlaybackStatus.LOADING -> stringResource(R.string.buffering)
                PlaybackStatus.ERROR ->
                    playerState.errorMessage ?: stringResource(R.string.error_unknown)
                PlaybackStatus.IDLE -> stringResource(R.string.tap_to_play)
            }

            Text(
                text = station.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = secondLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje de estado en lenguaje claro.
            val statusText = when (playerState.status) {
                PlaybackStatus.PLAYING -> stringResource(R.string.radio_live)
                PlaybackStatus.PAUSED -> stringResource(R.string.radio_paused)
                PlaybackStatus.LOADING -> stringResource(R.string.loading)
                PlaybackStatus.ERROR ->
                    playerState.errorMessage ?: stringResource(R.string.error_unknown)
                PlaybackStatus.IDLE -> stringResource(R.string.tap_to_play)
            }
            val statusColor = when (playerState.status) {
                PlaybackStatus.PLAYING -> MaterialTheme.colorScheme.primary
                PlaybackStatus.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                textAlign = TextAlign.Center
            )

            // Aviso del temporizador para dormir (Fase 5): tiempo restante.
            if (sleepTimerState.active) {
                val totalSeconds = sleepTimerState.remainingMillis / 1000
                val minutesLeft = totalSeconds / 60
                val secondsLeft = totalSeconds % 60
                Text(
                    text = stringResource(
                        R.string.sleep_timer_active,
                        String.format("%02d:%02d", minutesLeft, secondsLeft)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Botón central de reproducción.
            val isThisStation = playerState.currentStation?.url == station.url
            val isBuffering = isThisStation && playerState.status == PlaybackStatus.LOADING
            val isPlaying = isThisStation && playerState.status == PlaybackStatus.PLAYING

            FilledIconButton(
                onClick = { playerViewModel.togglePlay(station) },
                modifier = Modifier.size(84.dp),
                enabled = true
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
