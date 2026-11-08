/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.jjcampac.eter.R

/**
 * Marca de agua de fondo para las pantallas de listado de emisoras (Favoritas y Todas).
 *
 * Usa el propio icono del launcher de la app (`ic_launcher_foreground`, el logo de la
 * radio con sus ondas), renderizado grande y en un tono muy atenuado del onSurfaceVariant,
 * para que quede como sello de fondo sin estorbar la lectura de las tarjetas.
 */
@Composable
fun EterWatermark(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .aspectRatio(1f)
        )
    }
}