package com.jjcampac.eter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjcampac.eter.R
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.PlayerState
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
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val station = playerState.currentStation ?: return

    val isThisStation = playerState.currentStation?.url == station.url
    val isPlaying = isThisStation && playerState.status == PlaybackStatus.PLAYING
    val isBuffering = isThisStation && playerState.status == PlaybackStatus.LOADING

    val secondaryText = when {
        playerState.status == PlaybackStatus.ERROR -> {
            playerState.errorMessage ?: stringResource(R.string.error_unknown)
        }
        isBuffering -> stringResource(R.string.buffering)
        playerState.status == PlaybackStatus.PAUSED -> stringResource(R.string.radio_paused)
        playerState.metadataTitle.isNullOrEmpty() -> station.subtitle.ifEmpty { station.name }
        else -> playerState.metadataTitle.orEmpty()
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
            Icon(
                imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
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
                    text = secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
