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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjcampac.eter.ui.theme.EterShapePanel
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemePreviews
import kotlinx.coroutines.delay

/**
 * Aviso de error de conexión con los mismos colores y forma del mini reproductor.
 * Se muestra como un overlay compacto en la parte inferior de la pantalla, sobre
 * cualquier pestaña (Favoritas, Estaciones, Ajustes o reproductor).
 *
 * Se auto-oculta tras 4 segundos o al pulsar el botón de aceptar.
 *
 * @param message texto del error; si es nulo o vacío no se dibuja nada.
 * @param onDismiss se invoca al pasar los 4 segundos (y al pulsar aceptar).
 */
@Composable
fun ErrorToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message.isNullOrEmpty()) return

    // Auto-dismiss after 4 seconds, like a standard Toast.
    LaunchedEffect(message) {
        delay(4000)
        onDismiss()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .wrapContentWidth(),
            shape = EterShapePanel,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================
// Previsualizaciones
// ============================================================

/** Aviso de error de conexión centrado sobre el fondo de la app. */
@ThemePreviews
@Composable
private fun ErrorToastPreview() {
    EterTheme(themeMode = ThemeMode.SISTEMA) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            ErrorToast(
                message = "No se pudo conectar con la emisora. Comprueba tu conexión.",
                onDismiss = {}
            )
        }
    }
}
