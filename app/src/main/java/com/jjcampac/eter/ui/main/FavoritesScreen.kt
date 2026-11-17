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
package com.jjcampac.eter.ui.main

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import android.os.Vibrator
import android.os.VibrationEffect
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjcampac.eter.R
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.ui.components.EterWatermark
import com.jjcampac.eter.ui.components.SELECTED_ROW_Z_INDEX
import com.jjcampac.eter.ui.components.StationItem
import com.jjcampac.eter.ui.player.PlayerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val TAG = "FavoritesScreen"

/**
 * Pestaña "Favoritas": lista de emisoras marcadas como favoritas.
 *
 * Si no hay favoritas se muestra un mensaje de ayuda. El resto del contenido
 * (barra superior, mini reproductor y barra de navegación) lo gestiona el
 * Scaffold principal de la actividad.
 */
@Composable
fun FavoritesScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val favorites by mainViewModel.favorites.collectAsStateWithLifecycle()

    if (favorites.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Sin favoritas",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.favorites_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            // Marca de agua (dibujo representativo de la app de radio) de fondo.
            EterWatermark(modifier = Modifier.fillMaxSize())

            // Lista reordenable con drag & drop (pulsación larga para arrastrar).
            val lazyListState = rememberLazyListState()

            // Estado del arrastre a nivel pantalla: evita que la lista se resetee
            // si Room emite una nueva copia mientras el usuario está arrastrando.
            var isDragging by remember { mutableStateOf(false) }

            // Orden temporal aplicado solo durante el arrastre (pulsación larga).
            // Cuando es null, sortedFavorites es favorites directamente (siempre al
            // día con Room), de modo que las filas —y sus iconos— se pintan con los
            // datos actuales nada más entrar a la pantalla o volver de otra pestaña.
            var draggedOrder by remember { mutableStateOf<List<Station>?>(null) }

            // Lista mostrada: durante el arrastre se usa la copia reordenada; fuera
            // de arrastre se muestra favorites tal como llega de Room.
            val sortedFavorites: List<Station> = draggedOrder ?: favorites

            // Vibración háptica breve cuando empieza el arrastre.
            val context = LocalContext.current
            DisposableEffect(isDragging) {
                if (isDragging) {
                    // Algunos dispositivos no permiten vibrar (SecurityException p. ej.);
                    // la IU nunca debe crashear por la háptica.
                    try {
                        val vibrator = context.getSystemService(Vibrator::class.java)
                        vibrator?.vibrate(
                            VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Vibración al arrastrar no disponible: ${e.message}")
                    }
                }
                onDispose {}
            }

            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                // Durante el arrastre solo se modifica la copia temporal; NO se escribe
                // en Room en cada movimiento.
                val base = draggedOrder ?: sortedFavorites
                draggedOrder = base.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val stations = sortedFavorites
                items(stations, key = { it.id }) { station ->
                    ReorderableItem(
                        state = reorderableState,
                        key = station.id,
                        // La emisora en reproducción sube de capa para que su sombra
                        // proyectada se dibuje sobre la fila siguiente.
                        modifier = Modifier.zIndex(
                            if (playerState.currentStation?.url == station.url) {
                                SELECTED_ROW_Z_INDEX
                            } else {
                                0f
                            }
                        )
                    ) { itemIsDragging ->
                        // Emisora en reproducción: sube de capa para que la sombra
                        // proyectada se dibuje sobre la fila siguiente.
                        val esSeleccionada = playerState.currentStation?.url == station.url
                        // Mueve un paso la emisora (usado por las acciones de accesibilidad) y
                        // persiste el nuevo orden en una sola transacción de Room.
                        val moveOneStep: (Boolean) -> Boolean = { moveUp ->
                            val base = draggedOrder ?: sortedFavorites
                            val currentIndex = base.indexOfFirst { it.id == station.id }
                            val targetIndex = if (moveUp) currentIndex - 1 else currentIndex + 1
                            if (currentIndex == -1 ||
                                targetIndex < 0 ||
                                targetIndex >= base.size
                            ) {
                                false
                            } else {
                                val nuevaOrden = base.toMutableList().apply {
                                    val moved = removeAt(currentIndex)
                                    add(targetIndex, moved)
                                }
                                draggedOrder = nuevaOrden
                                mainViewModel.reorderFavorites(nuevaOrden.map { it.id })
                                true
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        isDragging = true
                                    },
                                    onDragStopped = {
                                        isDragging = false
                                        // Guarda el nuevo orden en una sola transacción de Room.
                                        val ordenFinal = draggedOrder ?: favorites
                                        mainViewModel.reorderFavorites(ordenFinal.map { it.id })
                                        draggedOrder = null
                                    }
                                )
                                .animateItem()
                                .then(if (itemIsDragging) Modifier.shadow(8.dp) else Modifier)
                                .semantics {
                                    // Accesibilidad: reordenar sin arrastrar (TalkBack).
                                    // Solo tiene sentido con más de una favorita.
                                    if (sortedFavorites.size > 1) {
                                        customActions = listOf(
                                            CustomAccessibilityAction("Mover arriba") {
                                                moveOneStep(true)
                                            },
                                            CustomAccessibilityAction("Mover abajo") {
                                                moveOneStep(false)
                                            }
                                        )
                                    }
                                }
                        ) {
                            StationItem(
                                station = station,
                                isCurrent = esSeleccionada,
                                isPlaying = playerState.status == PlaybackStatus.PLAYING &&
                                    esSeleccionada,
                                isPaused = esSeleccionada &&
                                    playerState.status == PlaybackStatus.PAUSED,
                                isSelected = esSeleccionada,
                                onClick = {
                                    if (!itemIsDragging) onStationClick(station)
                                },
                                onFavoriteToggle = { onToggleFavorite(station) },
                                modifier = Modifier
                            )
                        }
                    }
                }
            }

        }
    }
}
