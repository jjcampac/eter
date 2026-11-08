/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.util.AppLanguage
import com.jjcampac.eter.util.TestData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

/** País por defecto de la búsqueda en Radio Browser (España). */
private const val DEFAULT_COUNTRY = "España"

/** Espera tras la última pulsación antes de consultar la API (ms). */
private const val SEARCH_DEBOUNCE_MS = 500L

/** Origen de las emisoras mostradas en la pestaña "Estaciones". */
enum class SearchSource { LOCAL, INTERNET }

/** Estado de la pantalla principal. */
data class MainUiState(
    val query: String = "",
    val source: SearchSource = SearchSource.INTERNET,
    val country: String = DEFAULT_COUNTRY,
    val genre: String = "",
    val stations: List<Station> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val remoteStations: List<Station> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearchError: Boolean = false
) {
    /** Resultados remotos con su estado de favorita actualizado desde Room. */
    val internetStations: List<Station>
        get() = remoteStations.map { it.copy(isFavorite = it.id in favoriteIds) }
}

/**
 * ViewModel de la pantalla principal.
 *
 * Fase 3: catálogo local de Room (emisoras manuales y favoritas).
 * Fase 4: búsqueda en la API de Radio Browser con origen local/internet,
 * debounce de 500 ms, estados de cargando/error y persistencia de favoritas.
 * El país por defecto de la búsqueda se adapta al idioma de la UI
 * (se pasa desde la Activity al crear el ViewModel).
 */
class MainViewModel(
    private val repository: StationRepository,
    defaultCountry: String = DEFAULT_COUNTRY
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(country = defaultCountry))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = repository.getAllStations().firstOrNull { true }?.firstOrNull()
            if (existing == null) {
                // Sembrar datos de prueba la primera vez
                TestData.all.forEach { repository.addStation(it) }
            }
        }
        // Mantener en el estado los ids de las favoritas para marcar resultados remotos.
        viewModelScope.launch {
            repository.getFavorites().collect { favorites ->
                _uiState.update { it.copy(favoriteIds = favorites.map { f -> f.id }.toSet()) }
            }
        }
        // País por defecto según el idioma de la UI: al cambiar idioma en Ajustes
        // el campo "País" de la búsqueda se actualiza al instante (sin reiniciar).
        viewModelScope.launch {
            AppLanguage.currentLanguage.collect { langCode ->
                _uiState.update {
                    it.copy(country = AppLanguage.defaultCountryDisplayName(langCode))
                }
            }
        }

        // Catálogo local (Room): alimenta "Mis emisoras" y la pestaña de Favoritas.
        viewModelScope.launch {
            repository.getAllStations().collect { stations ->
                _uiState.update { it.copy(stations = stations) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        scheduleRemoteSearch()
    }

    fun onSourceChange(source: SearchSource) {
        _uiState.update { it.copy(source = source) }
        if (source == SearchSource.INTERNET) {
            scheduleRemoteSearch()
        } else {
            stopSearch()
        }
    }

    fun onCountryChange(country: String) {
        _uiState.update { it.copy(country = country) }
        scheduleRemoteSearch()
    }

    fun onGenreChange(genre: String) {
        _uiState.update { it.copy(genre = genre) }
        scheduleRemoteSearch()
    }

    /** Reintenta la búsqueda actual tras un error. */
    fun retrySearch() {
        val state = _uiState.value
        if (state.source == SearchSource.INTERNET && state.query.isNotBlank()) {
            searchJob = viewModelScope.launch { searchRemote() }
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            if (station.id in _uiState.value.favoriteIds) {
                repository.setFavorite(station.id, false)
            } else {
                // Las emisoras de Internet no están en Room: se guardan al marcarlas
                // como favoritas (upsert) y quedan accesibles sin conexión.
                repository.addStation(station.copy(isFavorite = true))
            }
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            repository.deleteStation(station)
        }
    }

    /**
     * Programa la búsqueda remota con debounce. Con la consulta vacía no se
     * consulta la API y se limpian los resultados previos.
     */
    private fun scheduleRemoteSearch() {
        searchJob?.cancel()
        val state = _uiState.value
        if (state.source != SearchSource.INTERNET || state.query.isBlank()) {
            _uiState.update {
                it.copy(isSearching = false, remoteStations = emptyList(), hasSearchError = false)
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchRemote()
        }
    }

    private fun stopSearch() {
        searchJob?.cancel()
        searchJob = null
        _uiState.update {
            it.copy(isSearching = false, remoteStations = emptyList(), hasSearchError = false)
        }
    }

    private suspend fun searchRemote() {
        _uiState.update { it.copy(isSearching = true, hasSearchError = false) }
        try {
            val state = _uiState.value
            val results = repository.searchRemote(
                query = state.query.trim(),
                country = state.country.trim(),
                genre = state.genre.trim().ifEmpty { null }
            )
            _uiState.update { it.copy(remoteStations = results, isSearching = false) }
        } catch (e: Exception) {
            Log.w(TAG, "searchRemote() error=${e.message}")
            _uiState.update {
                it.copy(isSearching = false, remoteStations = emptyList(), hasSearchError = true)
            }
        }
    }

    companion object {
        fun factory(
            repository: StationRepository,
            defaultCountry: String = DEFAULT_COUNTRY
        ): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository, defaultCountry) as T
                }
            }
    }
}
