package com.jjcampac.eter.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjcampac.eter.R
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.ui.components.StationItem
import com.jjcampac.eter.ui.player.PlayerViewModel

/**
 * Pestaña "Estaciones": buscador y catálogo completo de emisoras.
 *
 * El contenido (buscador + lista) se gestiona aquí, mientras que la barra
 * superior, el FAB de "añadir emisora" y la barra de navegación los aporta
 * el Scaffold principal de la actividad.
 */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onStationClick: (Station) -> Unit,
    onEditStation: (Station) -> Unit,
    onDeleteStation: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = mainViewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
            },
            singleLine = true
        )

        // Filtro local por nombre, país o género.
        val filtered = remember(uiState.query, uiState.stations) {
            val query = uiState.query.trim().lowercase()
            if (query.isEmpty()) {
                uiState.stations
            } else {
                uiState.stations.filter { station ->
                    station.name.lowercase().contains(query) ||
                        station.country?.lowercase()?.contains(query) == true ||
                        station.genre?.lowercase()?.contains(query) == true
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(filtered, key = { it.id }) { station ->
                    StationItem(
                        station = station,
                        isCurrent = playerState.currentStation?.url == station.url,
                        isPlaying = playerState.status == PlaybackStatus.PLAYING &&
                            playerState.currentStation?.url == station.url,
                        isPaused = playerState.currentStation?.url == station.url &&
                            playerState.status == PlaybackStatus.PAUSED,
                        onClick = { onStationClick(station) },
                        onFavoriteToggle = { onToggleFavorite(station) },
                        onEdit = if (station.isCustom) { { onEditStation(station) } } else null,
                        onDelete = if (station.isCustom) { { onDeleteStation(station) } } else null
                    )
                }
            }
        }
    }
}
