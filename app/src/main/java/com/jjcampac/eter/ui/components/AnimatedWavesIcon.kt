package com.jjcampac.eter.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedWavesIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val transition = rememberInfiniteTransition(label = "waves")
    val bars = listOf(
        transition.animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "b1"),
        transition.animateFloat(0.8f, 0.5f, infiniteRepeatable(tween(780), RepeatMode.Reverse), label = "b2"),
        transition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(620), RepeatMode.Reverse), label = "b3"),
        transition.animateFloat(0.9f, 0.45f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "b4"),
        transition.animateFloat(0.6f, 0.3f, infiniteRepeatable(tween(560), RepeatMode.Reverse), label = "b5")
    )
    Canvas(modifier = modifier.semantics { 
        if (contentDescription != null) this.contentDescription = contentDescription 
    }) {
        // Margenes internos para que las barras queden centradas y proporcionadas
        // (como un icono clasico), no ocupando todo el canvas.
        val n = 5
        val margin = size.width * 0.18f
        val useWidth = size.width - (2f * margin)
        val gap = useWidth / (n * 2f)
        val barWidth = gap * 0.6f
        val centerY = size.height / 2f
        val maxHeight = size.height * 0.6f
        val startX = margin
        for (i in 0 until n) {
            val amp = bars[i].value
            val h = maxHeight * amp
            val x = startX + i * gap + gap * 0.5f - barWidth / 2f
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
