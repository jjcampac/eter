/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjcampac.eter.R
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.ui.components.EterWatermark
import com.jjcampac.eter.ui.components.StationItem
import com.jjcampac.eter.ui.player.PlayerViewModel

/**
 * Pestaña "Estaciones": buscador y catálogo de emisoras.
 *
 * Permite alternar entre dos orígenes con *chips*: "Mis emisoras" (catálogo de
 * Room) e "Internet" (Radio Browser, con país y género). El contenido, la barra
 * superior, el FAB de "añadir emisora" y la barra de navegación los aporta el
 * Scaffold principal de la actividad.
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
        // Selector de origen de la búsqueda.
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.source == SearchSource.INTERNET,
                onClick = { mainViewModel.onSourceChange(SearchSource.INTERNET) },
                label = { Text(stringResource(R.string.search_source_internet)) }
            )
            FilterChip(
                selected = uiState.source == SearchSource.LOCAL,
                onClick = { mainViewModel.onSourceChange(SearchSource.LOCAL) },
                label = { Text(stringResource(R.string.search_source_local)) }
            )
        }

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

        // Filtros adicionales solo en búsqueda por Internet (país y género).
        if (uiState.source == SearchSource.INTERNET) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.country,
                    onValueChange = mainViewModel::onCountryChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.search_country_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.genre,
                    onValueChange = mainViewModel::onGenreChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.search_genre_label)) },
                    singleLine = true
                )
            }
        }


        when (uiState.source) {
            SearchSource.LOCAL -> LocalStationsSection(
                uiState = uiState,
                playerState = playerState,
                onStationClick = onStationClick,
                onEditStation = onEditStation,
                onDeleteStation = onDeleteStation,
                onToggleFavorite = onToggleFavorite
            )
            SearchSource.INTERNET -> InternetStationsSection(
                uiState = uiState,
                playerState = playerState,
                onStationClick = onStationClick,
                onToggleFavorite = onToggleFavorite,
                onRetry = mainViewModel::retrySearch
            )
        }
    }
}

/**
 * Lista "Mis Emisoras": muestran únicamente las emisoras añadidas a mano
 * (`isCustom`). Las que se guardan al marcarlas desde Internet solo salen en
 * Favoritas. Filtro por nombre, país o género.
 */
@Composable
private fun LocalStationsSection(
    uiState: MainUiState,
    playerState: com.jjcampac.eter.player.PlayerState,
    onStationClick: (Station) -> Unit,
    onEditStation: (Station) -> Unit,
    onDeleteStation: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit
) {
    val customStations = remember(uiState.stations) {
        uiState.stations.filter { it.isCustom }
    }

    // Filtro local por nombre, país o género.
    val filtered = remember(uiState.query, customStations) {
        val query = uiState.query.trim().lowercase()
        if (query.isEmpty()) {
            customStations
        } else {
            customStations.filter { station ->
                station.name.lowercase().contains(query) ||
                    station.country?.lowercase()?.contains(query) == true ||
                    station.genre?.lowercase()?.contains(query) == true
            }
        }
    }

    when {
        customStations.isEmpty() -> {
            EmptyStateMessage(text = stringResource(R.string.manual_stations_empty))
        }
        filtered.isEmpty() -> {
            EmptyStateMessage(text = stringResource(R.string.no_results))
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filtered, key = { it.id }) { station ->
                    // Las emisoras manuales se eliminan deslizando la tarjeta a la
                    // izquierda. Para que TalkBack pueda hacerlo sin gesto, se añade
                    // la misma acción a la propia fila.
                    val etiquetaEliminar = stringResource(R.string.delete_station)
                    SwipeToDelete(onDelete = { onDeleteStation(station) }) {
                        LocalStationRow(
                            station = station,
                            playerState = playerState,
                            onStationClick = onStationClick,
                            onToggleFavorite = onToggleFavorite,
                            onEditStation = onEditStation,
                            modifier = Modifier.semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(etiquetaEliminar) {
                                        onDeleteStation(station)
                                        true
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Resultados de la búsqueda en Radio Browser (Internet). */
@Composable
private fun InternetStationsSection(
    uiState: MainUiState,
    playerState: com.jjcampac.eter.player.PlayerState,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Marca de agua (dibujo representativo de la app de radio) de fondo.
        EterWatermark(modifier = Modifier.fillMaxSize())
        when {
        uiState.isSearching -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.hasSearchError -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.error_radio_browser),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
        uiState.query.isBlank() -> {
            EmptyStateMessage(text = stringResource(R.string.search_internet_hint))
        }
        uiState.internetStations.isEmpty() -> {
            EmptyStateMessage(text = stringResource(R.string.no_results))
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.internetStations, key = { it.id }) { station ->
                    StationItem(
                        station = station,
                        isCurrent = playerState.currentStation?.url == station.url,
                        isPlaying = playerState.status == PlaybackStatus.PLAYING &&
                            playerState.currentStation?.url == station.url,
                        isPaused = playerState.currentStation?.url == station.url &&
                            playerState.status == PlaybackStatus.PAUSED,
                        onClick = { onStationClick(station) },
                        onFavoriteToggle = { onToggleFavorite(station) },
                        onEdit = null
                    )
                }
            }
        }
    }
    }
}

/** Mensaje centrado para estados vacíos (sin resultados, sin consulta, etc.). */
@Composable
private fun EmptyStateMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Fila de una emisora de la lista local, con sus acciones (editar). */
@Composable
private fun LocalStationRow(
    station: Station,
    playerState: com.jjcampac.eter.player.PlayerState,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onEditStation: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier
    )
}

/**
 * Gesto de deslizamiento para eliminar una emisora.
 *
 * Solo se desliza de derecha a izquierda (se muestra el fondo rojo con el icono
 * de borrar); al cruzar el umbral se invoca [onDelete] y la fila se elimina.
 * Se usa únicamente con emisoras manuales en "Mis Emisoras".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    // Al superar el umbral hacia la izquierda, elimina la emisora.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_station),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    ) {
        content()
    }
}
