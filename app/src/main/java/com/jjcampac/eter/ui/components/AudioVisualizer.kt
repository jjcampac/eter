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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjcampac.eter.player.audio.AudioSpectrum
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemePreviews
import kotlinx.coroutines.isActive
import kotlin.math.abs

/**
 * Visualizador de audio real: barras verticales redondeadas que siguen el
 * espectro de lo que está sonando.
 *
 * Se usa en el mini reproductor, en la pantalla del reproductor ("En directo") y
 * en la tarjeta de la emisora en reproducción, cambiando solo tamaño, grosor y
 * espaciado.
 *
 * - Los niveles llegan de [AudioSpectrum] (publicados por el reproductor).
 * - Ataque casi instantáneo y caída lenta (~18 % por fotograma).
 * - Sin reproducción, las barras quedan en [minBarHeight].
 * - El suavizado y la lectura de los valores se hacen en la fase de dibujo, así
 *   que el mini reproductor, la pantalla y la lista no se recomponen 30 veces
 *   por segundo.
 *
 * @param barCount Número de barras. Si es menor que las 6 bandas del espectro,
 *   las bandas se agrupan proporcionalmente en lugar de recortarse.
 * @param barWidth Grosor de cada barra.
 * @param barGap Separación entre barras.
 * @param minBarHeight Altura mínima visible (sin reproducción).
 * @param active `true` mientras hay reproducción; al pasar a `false` las barras
 *   caen hasta la altura mínima.
 * @param contentDescription Texto para accesibilidad (nulo si es decorativo).
 */
@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    barCount: Int = AudioSpectrum.BANDAS,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    barGap: Dp = 2.dp,
    minBarHeight: Dp = 3.dp,
    active: Boolean = true,
    contentDescription: String? = null
) {
    AudioVisualizerNiveles(
        niveles = AudioSpectrum.niveles.collectAsStateWithLifecycle(),
        modifier = modifier,
        barCount = barCount,
        color = color,
        barWidth = barWidth,
        barGap = barGap,
        minBarHeight = minBarHeight,
        active = active,
        contentDescription = contentDescription
    )
}

/**
 * Variante que recibe los niveles como [State], para poder previsualizar el
 * componente con valores fijos sin depender del reproductor.
 */
@Composable
internal fun AudioVisualizerNiveles(
    niveles: State<FloatArray>,
    modifier: Modifier = Modifier,
    barCount: Int = AudioSpectrum.BANDAS,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    barGap: Dp = 2.dp,
    minBarHeight: Dp = 3.dp,
    active: Boolean = true,
    contentDescription: String? = null,
    nivelesIniciales: FloatArray? = null
) {
    val barras = barCount.coerceAtLeast(1)
    val alturas = remember(barras, nivelesIniciales) { AlturasBarras(barras, nivelesIniciales) }
    val destino = remember(barras) { FloatArray(barras) }

    // Un paso por fotograma: agrupa las bandas y avanza el suavizado. Si no hay
    // reproducción, deja caer las barras y detiene el bucle al llegar al mínimo.
    LaunchedEffect(barras, active) {
        while (isActive) {
            withFrameNanos { }
            agruparBandas(niveles.value, destino, active)
            val haCambiado = alturas.avanzar(destino)
            if (!active && !haCambiado) break
        }
    }

    Canvas(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        }
    ) {
        // Lectura en la fase de dibujo: un nivel nuevo solo invalida el dibujo.
        val valores = alturas.valores.value
        val anchoBarra = barWidth.toPx()
        val hueco = barGap.toPx()
        val altoMinimo = minBarHeight.toPx()
        val anchoTotal = barras * anchoBarra + (barras - 1) * hueco
        val inicioX = (size.width - anchoTotal) / 2f
        val centroY = size.height / 2f
        val recorrido = (size.height - altoMinimo).coerceAtLeast(0f)
        for (i in 0 until barras) {
            val altura = altoMinimo + recorrido * valores[i].coerceIn(0f, 1f)
            val x = inicioX + i * (anchoBarra + hueco)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centroY - altura / 2f),
                size = Size(anchoBarra, altura),
                cornerRadius = CornerRadius(anchoBarra / 2f)
            )
        }
    }
}

/**
 * Reparte las bandas del espectro en el número de barras pedido: con menos
 * barras que bandas se hace la media de cada grupo (no se recortan) y con más
 * se repiten proporcionalmente.
 */
private fun agruparBandas(origen: FloatArray, destino: FloatArray, activo: Boolean) {
    if (!activo) {
        destino.fill(0f)
        return
    }
    for (j in destino.indices) {
        val desde = j * origen.size / destino.size
        val hasta = ((j + 1) * origen.size / destino.size).coerceAtLeast(desde + 1)
        var suma = 0f
        var cuentas = 0
        for (b in desde until hasta.coerceAtMost(origen.size)) {
            suma += origen[b]
            cuentas++
        }
        destino[j] = if (cuentas > 0) suma / cuentas else 0f
    }
}

/**
 * Alturas suavizadas de las barras: ataque casi instantáneo y caída del 18 %
 * por fotograma. Los valores publicados solo se leen en el dibujo.
 */
private class AlturasBarras(cantidad: Int, iniciales: FloatArray? = null) {

    val valores = mutableStateOf(FloatArray(cantidad))

    private val actuales = FloatArray(cantidad)

    init {
        if (iniciales != null) {
            for (i in 0 until cantidad) {
                actuales[i] = iniciales.getOrElse(i) { 0f }.coerceIn(0f, 1f)
            }
            valores.value = actuales.copyOf()
        }
    }

    /** Avanza un fotograma hacia [destino]. Devuelve `true` si algo cambió. */
    fun avanzar(destino: FloatArray): Boolean {
        var haCambiado = false
        for (i in actuales.indices) {
            val actual = actuales[i]
            val objetivo = if (i < destino.size) destino[i] else 0f
            val factor = if (objetivo > actual) ATAQUE else CAIDA
            var siguiente = actual + (objetivo - actual) * factor
            // Se asienta en el objetivo para no arrastrar restos infinitos.
            if (abs(siguiente - objetivo) < UMBRAL_REPOSO) siguiente = objetivo
            if (siguiente != actual) {
                actuales[i] = siguiente
                haCambiado = true
            }
        }
        if (haCambiado) valores.value = actuales.copyOf()
        return haCambiado
    }

    private companion object {
        /** Subida: casi instantánea. */
        const val ATAQUE = 0.85f

        /** Bajada: 18 % por fotograma. */
        const val CAIDA = 0.18f

        /** Diferencia por debajo de la cual se considera que ya llegó. */
        const val UMBRAL_REPOSO = 0.004f
    }
}

// ============================================================
// Previsualizaciones
// ============================================================

/** Niveles de ejemplo para las previews (sin reproductor ni red). */
private val NivelesDeEjemplo = floatArrayOf(0.85f, 0.62f, 0.95f, 0.48f, 0.7f, 0.3f)

/** Contenedor común de las previews. */
@Composable
private fun AudioVisualizerPreviewContainer(
    barCount: Int,
    width: Dp,
    height: Dp,
    barWidth: Dp,
    barGap: Dp,
    minBarHeight: Dp,
    active: Boolean = true,
    niveles: FloatArray = NivelesDeEjemplo
) {
    EterTheme(themeMode = ThemeMode.SISTEMA) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                AudioVisualizerNiveles(
                    niveles = remember { mutableStateOf(niveles) },
                    barCount = barCount,
                    color = MaterialTheme.colorScheme.primary,
                    barWidth = barWidth,
                    barGap = barGap,
                    minBarHeight = minBarHeight,
                    active = active,
                    nivelesIniciales = niveles,
                    modifier = Modifier.size(width = width, height = height)
                )
            }
        }
    }
}

/** Tamaño del mini reproductor: 6 barras. */
@ThemePreviews
@Composable
private fun AudioVisualizerPreview() {
    AudioVisualizerPreviewContainer(
        barCount = AudioSpectrum.BANDAS,
        width = 28.dp,
        height = 28.dp,
        barWidth = 3.dp,
        barGap = 2.dp,
        minBarHeight = 3.dp
    )
}

/** Tamaño de la pantalla del reproductor: 6 barras gruesas. */
@ThemePreviews
@Composable
private fun AudioVisualizerGrandePreview() {
    AudioVisualizerPreviewContainer(
        barCount = AudioSpectrum.BANDAS,
        width = 90.dp,
        height = 64.dp,
        barWidth = 10.dp,
        barGap = 6.dp,
        minBarHeight = 8.dp
    )
}

/** Agrupación en 3 barras y estado sin reproducción (altura mínima). */
@ThemePreviews
@Composable
private fun AudioVisualizerAgrupadoParadoPreview() {
    AudioVisualizerPreviewContainer(
        barCount = 3,
        width = 33.dp,
        height = 33.dp,
        barWidth = 5.dp,
        barGap = 3.dp,
        minBarHeight = 3.dp,
        active = false,
        niveles = floatArrayOf(0f, 0f, 0f)
    )
}

