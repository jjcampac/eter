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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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

    // Fondo de la fila seleccionada = el mismo color de fondo del MiniPlayer:
    // su Surface usa `colorScheme.surface` con tonalElevation 12.dp, así que se
    // aplica el mismo tinte tonal para que ambos coincidan exactamente.
    val baseColor = if (isSelected) {
        colorScheme.surfaceColorAtElevation(SELECTED_SURFACE_TONAL_ELEVATION)
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

    // El contenido se desplaza 1.dp hacia abajo mientras se pulsa.
    val pressOffset by animateDpAsState(
        targetValue = if (isPressedState.value) PRESS_OFFSET else 0.dp,
        animationSpec = tween(durationMillis = PRESS_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
    )

    return this
        // La sombra proyectada se dibuja a mano en el drawBehind (más fiel al
        // diseño y sin las capas que recortaban el degradado).
        .drawBehind {
            // Los estados se leen dentro del dibujo para no recomponer al hacer scroll.
            val isPressed = isPressedState.value
            val selected = isSelected
            val factorClaro = if (isDarkTheme) 1f else LIGHT_THEME_ALPHA_FACTOR

            // 1. Sombra proyectada de la fila elevada: degradado intenso pegado al
            //    borde inferior (se dibuja fuera de la fila, sobre la fila vecina)
            //    y un halo tenue por encima, como en el diseño de referencia.
            if (selected && !isPressed) {
                val shadowHeight = PROJECTED_SHADOW_HEIGHT.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = PROJECTED_SHADOW_ALPHA * factorClaro),
                            Color.Black.copy(alpha = 0f)
                        ),
                        startY = size.height,
                        endY = size.height + shadowHeight
                    ),
                    topLeft = Offset(0f, size.height),
                    size = Size(size.width, shadowHeight)
                )
                val topShadowHeight = TOP_SHADOW_HEIGHT.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0f),
                            Color.Black.copy(alpha = TOP_SHADOW_ALPHA * factorClaro)
                        ),
                        startY = -topShadowHeight,
                        endY = 0f
                    ),
                    topLeft = Offset(0f, -topShadowHeight),
                    size = Size(size.width, topShadowHeight)
                )
            }

            // 2. Fondo de la fila, a todo el ancho.
            drawRect(color = color, size = size)

            // 3. Reflejo de luz de 1 dp en el canto superior (solo seleccionada).
            if (selected && !isPressed) {
                val lineHeight = LINE_HEIGHT.toPx()
                drawRect(
                    color = Color.White.copy(alpha = GLOW_ALPHA * factorClaro),
                    topLeft = Offset.Zero,
                    size = Size(size.width, lineHeight)
                )
            }

            // 4. Feedback de pulsación: sombra interior superior + línea de luz inferior.
            if (isPressed) {
                val shadowAlpha = INNER_SHADOW_ALPHA_DARK * factorClaro
                val innerHeight = INNER_SHADOW_HEIGHT.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = shadowAlpha),
                            Color.Black.copy(alpha = 0f)
                        ),
                        // Sin startY/endY el degradado se escala al alto del nodo
                        // completo, no al de este rectángulo.
                        startY = 0f,
                        endY = innerHeight
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, innerHeight)
                )
                val lineAlpha = LIGHT_LINE_ALPHA_DARK * factorClaro
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

/** Duración de la animación de color al cambiar la selección. */
private const val SELECTED_ANIMATION_DURATION_MS = 150

/** Duración de la animación de pulsación (hundimiento y vuelta). */
private const val PRESS_ANIMATION_DURATION_MS = 100

/** Desplazamiento hacia abajo del contenido mientras se pulsa. */
private val PRESS_OFFSET = 1.dp

/** Altura de la sombra interior superior al pulsar. */
private val INNER_SHADOW_HEIGHT = 11.dp

/** Altura del reflejo superior y de la línea de luz inferior (1 dp cada una). */
private val LINE_HEIGHT = 1.dp

/** Altura del degradado de sombra proyectada bajo la fila elevada. */
private val PROJECTED_SHADOW_HEIGHT = 14.dp

/** Altura del halo de sombra encima de la fila elevada. */
private val TOP_SHADOW_HEIGHT = 12.dp

/**
 * Elevación tonal aplicada al fondo de la fila seleccionada. Es la misma que
 * usa el MiniPlayer (tonalElevation = 12.dp) para que ambos colores coincidan.
 */
private val SELECTED_SURFACE_TONAL_ELEVATION = 12.dp

/** Alfa de la sombra interior superior en tema oscuro (en claro, la mitad). */
private const val INNER_SHADOW_ALPHA_DARK = 0.73f

/** Alfa del reflejo superior (blanco). */
private const val GLOW_ALPHA = 0.14f

/** Alfa de la línea de luz inferior en tema oscuro (en claro, la mitad). */
private const val LIGHT_LINE_ALPHA_DARK = 0.05f

/** Alfa de la sombra proyectada pegada al borde inferior de la fila elevada. */
private const val PROJECTED_SHADOW_ALPHA = 0.68f

/** Alfa del halo de sombra encima de la fila elevada. */
private const val TOP_SHADOW_ALPHA = 0.22f

/** En tema claro las sombras y luces se suavizan a la mitad. */
private const val LIGHT_THEME_ALPHA_FACTOR = 0.5f


/** zIndex con el que sube de capa la fila seleccionada en su lista. */
const val SELECTED_ROW_Z_INDEX = 1f
