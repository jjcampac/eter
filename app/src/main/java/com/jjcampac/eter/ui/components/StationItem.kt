package com.jjcampac.eter.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjcampac.eter.domain.model.Station

/**
 * Icono de ondas de sonido animadas (ecualizador).
 * Dibuja 5 barras con alturas animadas de forma asincrona mediante
 * una [rememberInfiniteTransition]. 100% Compose, sin dependencias extra.
 */
@Composable
fun AnimatedWavesIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Emisora en reproducción"
) {
    val transition = rememberInfiniteTransition(label = "waves")

    // 5 barras con distintas amplitudes y duraciones para un efecto organico
    val bars = listOf(
        transition.animateFloat(
            initialValue = 0.35f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar1"
        ),
        transition.animateFloat(
            initialValue = 0.8f, targetValue = 0.5f,
            animationSpec = infiniteRepeatable(tween(780, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar2"
        ),
        transition.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(620, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar3"
        ),
        transition.animateFloat(
            initialValue = 0.9f, targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar4"
        ),
        transition.animateFloat(
            initialValue = 0.6f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar5"
        )
    ).map { it.value }

    Canvas(
        modifier = modifier
            .size(24.dp)
            .semantics { this.contentDescription = contentDescription.orEmpty() }
    ) {
        val n = 5
        val gap = size.width / (n * 2f)
        val barWidth = gap * 0.6f
        val centerY = size.height / 2f
        val maxHeight = size.height * 0.9f

        for (i in 0 until n) {
            val amp = bars[i]
            val h = maxHeight * amp
            val x = i * gap + gap * 0.5f - barWidth / 2f
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

/**
 * Fila de una emisora en las listas de la app.
 *
 * @param isCurrent  `true` si es la emisora cargada actualmente en el reproductor.
 * @param isPlaying  `true` si ademas de ser la actual esta sonando.
 * @param isPaused   `true` si la emisora actual esta pausada.
 */
@Composable
fun StationItem(
    station: Station,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = containerColor),
        leadingContent = {
            Icon(
                imageVector = if (isCurrent) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                contentDescription = if (isCurrent) "Reproduciendo" else null,
                tint = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = station.subtitle.ifEmpty { station.url },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (station.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorita",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else if (isPlaying) {
                AnimatedWavesIcon(
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else if (isPaused) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Pausa",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    )
}
