/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jjcampac.eter.ui.theme.EterShapeInitials

/**
 * Carátula para la pantalla completa del reproductor (paso c).
 *
 * Prioridad:
 *  1. Carátula incrustada en los metadatos del stream (Media3): `artworkData` (bytes)
 *     o `artworkUri` cuando el contenedor la entrega. Se carga con Coil.
 *  2. Si no hay carátula o su decodificación falla, se muestra el icono de la emisora
 *     con su cadena de respaldo completa ([EstacionIcono]).
 *
 * Esta carátula NO se usa en las listas de emisoras ni en el mini reproductor: solo en
 * el reproductor a pantalla completa (y la notificación la construye el servicio).
 *
 * @param streamArtworkData Carátula del stream como bytes (`MediaMetadata.artworkData`).
 * @param streamArtworkUri  Carátula del stream como URI (`MediaMetadata.artworkUri`).
 * @param shape             Forma de la carátula; por defecto esquinas de 12 dp.
 */
@Composable
fun CaratulaStream(
    nombre: String,
    imageUrl: String?,
    streamArtworkData: ByteArray?,
    streamArtworkUri: Uri?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = EterShapeInitials
) {
    val artworkBytes = streamArtworkData?.takeIf { it.isNotEmpty() }
    val hayCaratula = artworkBytes != null || streamArtworkUri != null

    // Sin carátula de stream: icono de la emisora con su cadena de respaldo.
    if (!hayCaratula) {
        EstacionIcono(
            nombre = nombre,
            imageUrl = imageUrl,
            size = size,
            modifier = modifier,
            contentDescription = contentDescription,
            shape = shape
        )
        return
    }

    // Mismo aspecto de caja que el icono de emisora: fondo surfaceVariant + la forma dada.
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = artworkBytes ?: streamArtworkUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        ) {
            when (painter.state.value) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent(
                    modifier = Modifier.fillMaxSize()
                )
                // La carátula del stream ha fallado: se cae al icono de la emisora.
                else -> EstacionIcono(
                    nombre = nombre,
                    imageUrl = imageUrl,
                    size = size,
                    contentDescription = contentDescription,
                    shape = shape
                )
            }
        }
    }
}
