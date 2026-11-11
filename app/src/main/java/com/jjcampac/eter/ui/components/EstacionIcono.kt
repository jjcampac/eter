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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jjcampac.eter.ui.theme.EterAccentTextColor
import com.jjcampac.eter.ui.theme.EterShapeInitials

/**
 * Icono de una emisora con cadena de respaldo y caché (Coil 3).
 *
 * Cadena de respaldo, en orden:
 *  1. Imagen de la emisora (URL de icono): se carga con [SubcomposeAsyncImage] de Coil 3.
 *     Si no hay URL, la descarga falla (404, timeout, formato) o aún está cargando,
 *     se muestra en su lugar el icono con las [calcularIniciales] de la emisora.
 *  2. Icono de iniciales: cuadrado con esquinas de 12 dp, fondo `surfaceVariant` y
 *     letras en el color de acento en negrita.
 *
 * Comportamiento al cargar: mientras la imagen se descarga se muestra directamente
 * el icono de iniciales (sin spinner ni parpadeos). El icono es decorativo si
 * [contentDescription] es `null`/vacío — el nombre de la emisora ya aparece en el
 * texto contiguo de las listas.
 *
 * Es el ÚNICO lugar con la lógica de icono de emisora: se reutiliza en la lista de
 * Estaciones, Favoritas, mini reproductor y pantalla del reproductor.
 *
 * @param nombre   Nombre de la emisora, del que se derivan las iniciales de respaldo.
 * @param imageUrl URL del logo de la emisora (`null` o vacía si no tiene).
 * @param size     Lado del cuadrado del icono (los tamaños cambian por contexto de uso).
 * @param shape    Forma del cuadrado. Por defecto 12 dp (listas); la carátula grande
 *                 del reproductor usa esquinas más redondeadas.
 */
@Composable
fun EstacionIcono(
    nombre: String,
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = EterShapeInitials
) {
    val iniciales = remember(nombre) { calcularIniciales(nombre) }
    val fontSize = (size.value * 0.4f).sp
    val hayUrl = !imageUrl.isNullOrBlank()

    // El fondo surfaceVariant queda siempre detrás: si la imagen tiene transparencia
    // o aún no ha cargado, el aspecto es consistente con el icono de iniciales.
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        if (hayUrl) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            ) {
                when (painter.state.value) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxSize()
                    )
                    // Vacío, cargando o error -> icono de iniciales (sin parpadeos).
                    else -> InicialesBox(iniciales, fontSize)
                }
            }
        } else {
            InicialesBox(iniciales, fontSize)
        }
    }
}


/**
 * Cuadrado de iniciales de respaldo: fondo `surfaceVariant`, letras de acento en negrita.
 * Solo se usa como respaldo visual; no tiene sentido semántico propio.
 */
@Composable
private fun InicialesBox(
    iniciales: String,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales,
            color = EterAccentTextColor(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize
        )
    }
}

/**
 * Calcula las iniciales de la emisora para el icono de respaldo.
 *
 * Reglas:
 *  - Se eliminan símbolos y signos; se conservan letras, dígitos y espacios.
 *  - Se ignoran las palabras que NO contienen ninguna letra (números sueltos, emojis),
 *    de modo que "RNE Radio 3" -> "RR" (el "3" no aporta) y "40 Principales" -> "PR".
 *  - Se toma la inicial (mayúscula) de hasta las dos primeras palabras con letra.
 *    Los artículos se conservan porque en nombres reales forman parte del acrónimo
 *    (ej. "La Ser" -> "LS", tal como pide el requisito).
 *  - Si solo hay una palabra significativa, se toman sus dos primeras letras
 *    (ej. "Radio" -> "RA") para no dejar una sola letra.
 *  - Caso límite: nombre vacío o sin letras -> "?".
 */
internal fun calcularIniciales(nombre: String): String {
    // Normaliza: cualquier carácter que no sea letra, dígito o espacio se vuelve espacio.
    val sinSimbolos = nombre.map { ch ->
        if (ch.isLetterOrDigit() || ch.isWhitespace()) ch else ' '
    }.joinToString("")

    val palabras = sinSimbolos.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (palabras.isEmpty()) return "?"

    // Solo las palabras con al menos una letra aportan inicial.
    val significativas = palabras.filter { p -> p.any { it.isLetter() } }
    val candidatas = significativas.ifEmpty { palabras }

    val b = StringBuilder()
    for (palabra in candidatas) {
        if (b.length >= 2) break
        val inicial = palabra.first()
        if (inicial.isLetter()) {
            b.append(inicial.uppercaseChar())
        }
    }
    if (b.isEmpty()) return "?"

    // Una única palabra significativa: usa sus dos primeras letras.
    if (b.length == 1 && candidatas.size == 1) {
        val primeras = candidatas.first().take(2)
        if (primeras.length >= 2) {
            b.append(primeras[1].uppercaseChar())
        }
    }
    return b.toString()
}
