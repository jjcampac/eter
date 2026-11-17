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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.ui.PreviewData
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemePreviews
import com.jjcampac.eter.ui.theme.EterSurfaceCardDark
import com.jjcampac.eter.ui.theme.EterSurfaceCardLight
import com.jjcampac.eter.ui.theme.LocalEterDarkTheme

/**
 * Fila de una emisora en las listas de la app.
 *
 * Usa exclusivamente colores de MaterialTheme.colorScheme y formas de ui/theme.
 * No hay colores sueltos en esta pantalla.
 *
 * @param isCurrent  true si es la emisora cargada actualmente en el reproductor.
 * @param isPlaying  true si ademas de ser la actual esta sonando.
 * @param isPaused   true si la emisora actual esta pausada.
 */
@Composable
fun StationItem(
    station: Station,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()

    StationItemContent(
        station = station,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        isPaused = isPaused,
        isSelected = isSelected,
        interactionSource = interactionSource,
        isPressedState = isPressedState,
        onClick = onClick,
        onFavoriteToggle = onFavoriteToggle,
        onEdit = onEdit,
        modifier = modifier
    )
}

/**
 * Contenido de la fila de una emisora en las listas de la app.
 *
 * Se expone por separado de [StationItem] para poder previsualizar los estados
 * normal, seleccionada y pulsada sin depender de un [MutableInteractionSource]
 * real.
 *
 * Usa exclusivamente colores de MaterialTheme.colorScheme y formas de ui/theme.
 * No hay colores sueltos en esta pantalla.
 *
 * @param isSelected true si la fila es la emisora seleccionada/en reproducción.
 * @param interactionSource fuente de interacciones de la fila (una por fila).
 * @param isPressedState estado de pulsación observado de [interactionSource].
 */
@Composable
fun StationItemContent(
    station: Station,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    isSelected: Boolean = false,
    interactionSource: MutableInteractionSource,
    isPressedState: State<Boolean>,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isHighlighted = isCurrent || isPlaying || isPaused
    val isDarkTheme = LocalEterDarkTheme.current
    val cardColor = if (isDarkTheme) EterSurfaceCardDark else EterSurfaceCardLight
    val accentBarColor = MaterialTheme.colorScheme.primary

    // Foco de teclado/D-pad (el toque ya no muestra indicación de ripple).
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        // Toda la fila es pulsable: selecciona y reproduce la emisora (y hace
        // aparecer el mini reproductor). Los IconButton de la derecha consumen
        // sus propios toques, así que no interfieren.
        modifier = modifier
            .fillMaxWidth()
            .stationRowEffect(isSelected = isSelected, isPressedState = isPressedState)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Sin ripple táctil: la retroalimentación es el efecto hundido de stationRowEffect.
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .then(
                if (isFocused) {
                    // Indicador de foco accesible solo para teclado o D-pad.
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                }
            )
            .semantics { selected = isSelected },
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isHighlighted) {
                        // Barra de acento en el borde izquierdo (altura completa).
                        Modifier.drawBehind {
                            drawRect(
                                color = accentBarColor,
                                size = Size(4.dp.toPx(), size.height)
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Icono de la emisora con cadena de respaldo (imagen o iniciales). Es decorativo:
            // el nombre de la emisora ya está en el texto contiguo. La emisora actual/en
            // reproducción añade una descripción informativa para accesibilidad.
            EstacionIcono(
                nombre = station.name,
                imageUrl = station.imageUrl,
                size = 40.dp,
                contentDescription = if (station.imageUrl.isNullOrBlank())
                    null else if (isCurrent || isPlaying) "Emisora actual" else null
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))

            // Información textual
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.subtitle.ifEmpty { station.url },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Estado de reproducción (olas animadas o pausa)
            if (isPlaying) {
                AnimatedWavesIcon(
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = "En directo",
                    modifier = Modifier.size(33.dp)
                )
            } else if (isPaused) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Pausa",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))

            // Acciones a la derecha (favorita y editar)
            Row(
                modifier = Modifier.padding(start = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Favorita toggle
                if (onFavoriteToggle != null) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (station.isFavorite) "Quitar de favoritas" else "Añadir a favoritas",
                            tint = if (station.isFavorite)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (station.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorita",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // Acciones para emisoras manuales
                if (station.isCustom && onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Editar",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}



// ============================================================
// Previsualizaciones
// ============================================================

/**
 * Envoltorio común de las previews de la fila: tema de la app, fondo y una
 * fuente de interacciones falsa para poder fijar el estado de pulsación.
 */
@Composable
private fun StationItemPreviewContainer(
    station: Station = PreviewData.copeSevilla,
    isSelected: Boolean = false,
    isPressed: Boolean = false,
    isPlaying: Boolean = false
) {
    EterTheme(themeMode = ThemeMode.SISTEMA) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            StationItemContent(
                station = station,
                isSelected = isSelected,
                isPlaying = isPlaying,
                interactionSource = remember { MutableInteractionSource() },
                isPressedState = remember { mutableStateOf(isPressed) },
                onClick = {},
                onFavoriteToggle = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

/** Fila normal (reposo) de una emisora. */
@ThemePreviews
@Composable
private fun StationItemNormalPreview() = StationItemPreviewContainer()

/** Fila seleccionada (la emisora está cargada en el reproductor). */
@ThemePreviews
@Composable
private fun StationItemSeleccionadaPreview() = StationItemPreviewContainer(
    isSelected = true,
    isPlaying = true
)

/** Fila pulsada (efecto hundido). */
@ThemePreviews
@Composable
private fun StationItemPulsadaPreview() = StationItemPreviewContainer(isPressed = true)

/** Fila de una emisora manual: muestra el botón de editar además del corazón. */
@ThemePreviews
@Composable
private fun StationItemEmisoraManualPreview() {
    EterTheme(themeMode = ThemeMode.SISTEMA) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            StationItemContent(
                station = PreviewData.emisoraManual,
                isSelected = false,
                interactionSource = remember { MutableInteractionSource() },
                isPressedState = remember { mutableStateOf(false) },
                onClick = {},
                onFavoriteToggle = {},
                onEdit = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}
