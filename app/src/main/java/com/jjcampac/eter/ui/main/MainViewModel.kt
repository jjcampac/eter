package com.jjcampac.eter.ui.main

import androidx.lifecycle.ViewModel
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.util.TestData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado de la pantalla principal. */
data class MainUiState(
    val query: String = "",
    val stations: List<Station> = emptyList()
)

/**
 * ViewModel de la pantalla principal.
 *
 * Fase 1: catálogo estático de prueba con una sola emisora y filtro local por nombre.
 * Fases 3 y 4: se conectará al repositorio (Room + Radio Browser).
 */
class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(stations = TestData.all)
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }
}
