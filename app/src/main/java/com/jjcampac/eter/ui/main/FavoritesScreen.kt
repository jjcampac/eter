/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjcampac.eter.R
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.ui.components.EterWatermark
import com.jjcampac.eter.ui.components.StationItem
import com.jjcampac.eter.ui.player.PlayerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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

    val favorites = uiState.stations.filter { it.isFavorite }

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
            var sortedFavorites by remember { mutableStateOf(favorites) }
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                sortedFavorites = sortedFavorites.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
                // Persiste el nuevo orden en Room tras cada movimiento.
                mainViewModel.reorderFavorites(sortedFavorites.map { it.id })
            }

            // Sincronizar con Room cuando cambien las favoritas externamente.
            androidx.compose.runtime.LaunchedEffect(favorites) {
                sortedFavorites = favorites
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(sortedFavorites.size) { index ->
                    val station = sortedFavorites[index]
                    ReorderableItem(
                        state = reorderableState,
                        key = station.id
                    ) { isDragging ->
                        Box(modifier = Modifier.longPressDraggableHandle()) {
                            StationItem(
                                station = station,
                                isCurrent = playerState.currentStation?.url == station.url,
                                isPlaying = playerState.status == PlaybackStatus.PLAYING &&
                                    playerState.currentStation?.url == station.url,
                                isPaused = playerState.currentStation?.url == station.url &&
                                    playerState.status == PlaybackStatus.PAUSED,
                                onClick = { onStationClick(station) },
                                onFavoriteToggle = { onToggleFavorite(station) }
                            )
                        }
                    }
                }
            }

        }
    }
}
