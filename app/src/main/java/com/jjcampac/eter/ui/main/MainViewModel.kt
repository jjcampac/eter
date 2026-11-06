package com.jjcampac.eter.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.util.TestData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/** Estado de la pantalla principal. */
data class MainUiState(
    val query: String = "",
    val stations: List<Station> = emptyList()
)

/**
 * ViewModel de la pantalla principal.
 *
 * Fase 3: conectado a Room mediante [StationRepository].
 */
class MainViewModel(private val repository: StationRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            val existing = repository.getAllStations().firstOrNull { true }?.firstOrNull()
            if (existing == null) {
                // Sembrar datos de prueba la primera vez
                TestData.all.forEach { repository.addStation(it) }
            }
        }
    }

    private val _query = MutableStateFlow("")

    val uiState: StateFlow<MainUiState> = combine(
        _query,
        repository.getAllStations()
    ) { query, stations ->
        MainUiState(query = query, stations = stations)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            repository.setFavorite(station.id, !station.isFavorite)
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            repository.deleteStation(station)
        }
    }

    companion object {
        fun factory(repository: StationRepository): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
    }
}
