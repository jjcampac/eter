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
        // 5 barras centradas horizontalmente dentro del canvas (con margen)
        val n = 5
        val margin = size.width * 0.18f
        val useWidth = size.width - (2f * margin)
        // gap entre centros de barras adyacentes
        val gap = useWidth / (n + 1f)
        val barWidth = gap * 0.5f
        val centerY = size.height / 2f
        val maxHeight = size.height * 0.6f
        // ancho total del bloque de barras
        val totalBarsWidth = (n - 1) * gap + barWidth
        val startX = (size.width - totalBarsWidth) / 2f
        for (i in 0 until n) {
            val amp = bars[i].value
            val h = maxHeight * amp
            val x = startX + i * gap
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
