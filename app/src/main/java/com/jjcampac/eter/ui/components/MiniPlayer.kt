/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
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
            // Logo corporativo de la emisora (favicon) o nota musical si no hay.
            if (station.imageUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                MiniPlayerLogo(
                    imageUrl = station.imageUrl,
                    name = station.name,
                    fallbackIcon = Icons.Filled.MusicNote,
                    fallbackTint = MaterialTheme.colorScheme.primary
                )
            }
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
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play)
                )
            }
        }
    }
}

/**
 * Logo corporativo de la emisora en el minireproductor, cargado con Coil.
 *
 * Se muestra en una miniatura redondeada. Mientras carga o si falla, muestra el
 * [fallbackIcon] (la nota musical) como respaldo.
 */
@Composable
private fun MiniPlayerLogo(
    imageUrl: String,
    name: String,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    fallbackTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(imageUrl)
    val state by painter.state.collectAsState()

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is AsyncImagePainter.State.Error,
            is AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading -> Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = fallbackTint,
                modifier = Modifier.size(24.dp)
            )

            is AsyncImagePainter.State.Success -> Image(
                painter = painter,
                contentDescription = "Logo de $name",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            )
        }
    }
}
