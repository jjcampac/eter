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
package com.jjcampac.eter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjcampac.eter.R
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.PlayerState
import com.jjcampac.eter.player.SleepTimer
import com.jjcampac.eter.ui.theme.EterShapePanel

/**
 * Mini reproductor compacto que se muestra sobre la barra de navegación.
 *
 * Aparece siempre que haya una emisora cargada en el reproductor. Al pulsar
 * sobre el cuerpo abre la pantalla completa; el botón derecho reproduce/pausa.
 * Usa exclusivamente colores de MaterialTheme.colorScheme y formas de ui/theme.
 */
@Composable
fun MiniPlayer(
    playerState: PlayerState,
    sleepTimer: SleepTimer,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val station = playerState.currentStation ?: return

    val isThisStation = playerState.currentStation?.url == station.url
    val isPlaying = isThisStation && playerState.status == PlaybackStatus.PLAYING

    val sleepTimerState by sleepTimer.state.collectAsState()

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = EterShapePanel,
        tonalElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la emisora con cadena de respaldo (imagen o iniciales). Decorativo:
            // el nombre de la emisora está en el texto contiguo del mini reproductor.
            EstacionIcono(
                nombre = station.name,
                imageUrl = station.imageUrl,
                size = 36.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
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
            }
// Icono animado de ondas mientras suena, a la izquierda del botón.
            if (isPlaying) {
                AnimatedWavesIcon(
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = stringResource(R.string.radio_live),
                    modifier = Modifier.size(28.dp)
                )
            }
            // Temporizador para dormir: icono de reloj y tiempo restante.
            if (sleepTimerState.active) {
                val totalSecs = sleepTimerState.remainingMillis / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = stringResource(R.string.sleep_timer),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = String.format("%d:%02d", mins, secs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play)
                )
            }
        }
    }
}

