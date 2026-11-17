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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jjcampac.eter.ui.theme.EterSurfaceCardDark
import com.jjcampac.eter.ui.theme.EterSurfaceCardLight
import com.jjcampac.eter.ui.theme.LocalEterDarkTheme
import kotlin.math.roundToInt

/**
 * Efecto visual elevado/hundido para las filas de emisora ([StationItem]).
 *
 * Encapsula toda la pintura especial (sombra proyectada, reflejo superior,
 * sombra interior al pulsar, línea de luz inferior y desplazamiento del
 * contenido) para que el composable de la fila solo se ocupe de su contenido.
 *
 * Las lecturas de [isPressedState] se hacen dentro de las lambdas de dibujo y
 * de desplazamiento para que, durante el scroll, la fila no se recomponga
 * solo por cambios de estado de pulsación.
 *
 * @param isSelected true si la fila es la emisora seleccionada/en reproducción.
 * @param isPressedState estado de pulsación de la fila.
 */
@Composable
fun Modifier.stationRowEffect(
    isSelected: Boolean,
    isPressedState: State<Boolean>,
): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = LocalEterDarkTheme.current

    // Fondo de la fila seleccionada = el mismo color de fondo del MiniPlayer
    // (MaterialTheme.colorScheme.surface). Se reutiliza el token del tema:
    // nunca se crea un color nuevo.
    val baseColor = if (isSelected) {
        colorScheme.surface
    } else if (isDarkTheme) {
        EterSurfaceCardDark
    } else {
        EterSurfaceCardLight
    }

    val color by animateColorAsState(
        targetValue = baseColor,
        animationSpec = tween(
            durationMillis = SELECTED_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing
        )
    )

    // Mientras se pulsa no hay sombra proyectada; si está seleccionada, 8.dp;
    // el resto de filas, 0.dp.
    val elevation by animateDpAsState(
        targetValue = if (isPressedState.value) {
            0.dp
        } else if (isSelected) {
            SELECTED_ELEVATION
        } else {
            0.dp
        },
        animationSpec = if (isPressedState.value) {
            tween(durationMillis = PRESS_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = SELECTED_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
        }
    )

    // El contenido se desplaza 1.dp hacia abajo mientras se pulsa.
    val pressOffset by animateDpAsState(
        targetValue = if (isPressedState.value) PRESS_OFFSET else 0.dp,
        animationSpec = tween(durationMillis = PRESS_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
    )

    return this
        // Solo la fila seleccionada sube de capa, para que su sombra se dibuje
        // por encima de las filas vecinas; el resto queda en su capa normal.
        .zIndex(if (isSelected) SELECTED_Z_INDEX else 0f)
        .shadow(
            elevation = elevation,
            shape = RectangleShape,
            clip = false, // La sombra no se recorta: se dibuja sobre las filas vecinas.
            ambientColor = Color.Black.copy(alpha = SHADOW_AMBIENT_ALPHA),
            spotColor = Color.Black.copy(alpha = SHADOW_SPOT_ALPHA)
        )
        .drawBehind {
            // Los estados se leen dentro del dibujo para no recomponer al hacer scroll.
            val isPressed = isPressedState.value
            val selected = isSelected

            // 1. Fondo de la fila, a todo el ancho.
            drawRect(color = color, size = size)

            // 2. Reflejo de luz de 1 dp en el canto superior (solo seleccionada).
            if (selected && !isPressed) {
                val lineHeight = LINE_HEIGHT.toPx()
                drawRect(
                    color = Color.White.copy(alpha = GLOW_ALPHA),
                    topLeft = Offset.Zero,
                    size = Size(size.width, lineHeight)
                )
            }

            // 3. Feedback de pulsación: sombra interior superior + línea de luz inferior.
            if (isPressed) {
                val shadowAlpha = if (isDarkTheme) INNER_SHADOW_ALPHA_DARK else INNER_SHADOW_ALPHA_DARK / 2f
                val innerHeight = INNER_SHADOW_HEIGHT.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = shadowAlpha),
                            Color.Black.copy(alpha = 0f)
                        )
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, innerHeight)
                )
                val lineAlpha = if (isDarkTheme) LIGHT_LINE_ALPHA_DARK else LIGHT_LINE_ALPHA_DARK / 2f
                val lineHeight = LINE_HEIGHT.toPx()
                drawRect(
                    color = Color.White.copy(alpha = lineAlpha),
                    topLeft = Offset(0f, size.height - lineHeight),
                    size = Size(size.width, lineHeight)
                )
            }
        }
        .offset { IntOffset(x = 0, y = pressOffset.toPx().roundToInt()) }
}

/** Duración de la animación de elevación y color al cambiar la selección. */
private const val SELECTED_ANIMATION_DURATION_MS = 150

/** Duración de la animación de pulsación (hundimiento y vuelta). */
private const val PRESS_ANIMATION_DURATION_MS = 100

/** Elevación proyectada de la fila seleccionada. */
private val SELECTED_ELEVATION = 8.dp

/** Desplazamiento hacia abajo del contenido mientras se pulsa. */
private val PRESS_OFFSET = 1.dp

/** Altura de la sombra interior superior al pulsar. */
private val INNER_SHADOW_HEIGHT = 12.dp

/** Altura del reflejo superior y de la línea de luz inferior (1 dp cada una). */
private val LINE_HEIGHT = 1.dp

/** Alfa del reflejo superior (blanco). */
private const val GLOW_ALPHA = 0.12f

/** Alfa de la sombra interior superior en tema oscuro (en claro, la mitad). */
private const val INNER_SHADOW_ALPHA_DARK = 0.45f

/** Alfa de la línea de luz inferior en tema oscuro (en claro, la mitad). */
private const val LIGHT_LINE_ALPHA_DARK = 0.08f

/** Alfa de la sombra proyectada (luz ambiente). */
private const val SHADOW_AMBIENT_ALPHA = 0.20f

/** Alfa de la sombra proyectada (punto focal). */
private const val SHADOW_SPOT_ALPHA = 0.35f

/** zIndex para que la fila seleccionada se dibuje sobre las filas vecinas. */
private const val SELECTED_Z_INDEX = 1f
