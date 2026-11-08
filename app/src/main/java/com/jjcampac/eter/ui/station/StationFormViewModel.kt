/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.station

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Estado del formulario de alta/edición de emisora. */
data class StationFormUiState(
    val id: String? = null,
    val name: String = "",
    val url: String = "",
    val country: String = "",
    val genre: String = "",
    val isCustom: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() = name.isNotBlank() && url.isNotBlank() && url.startsWith("http")
}

/**
 * ViewModel del formulario de alta/edición de emisora.
 */
class StationFormViewModel(
    private val repository: StationRepository,
    private val stationId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationFormUiState(isEditing = stationId != null))
    val uiState: StateFlow<StationFormUiState> = _uiState.asStateFlow()

    init {
        if (stationId != null) {
            viewModelScope.launch {
                repository.getStation(stationId)?.let { station ->
                    _uiState.value = _uiState.value.copy(
                        id = station.id,
                        name = station.name,
                        url = station.url,
                        country = station.country ?: "",
                        genre = station.genre ?: "",
                        isCustom = station.isCustom
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value, error = null) }
    fun onUrlChange(value: String) { _uiState.value = _uiState.value.copy(url = value, error = null) }
    fun onCountryChange(value: String) { _uiState.value = _uiState.value.copy(country = value) }
    fun onGenreChange(value: String) { _uiState.value = _uiState.value.copy(genre = value) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.value = state.copy(error = "Nombre y URL válida (http/https) son obligatorios")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            val station = Station(
                id = state.id ?: UUID.randomUUID().toString(),
                name = state.name.trim(),
                url = state.url.trim(),
                country = state.country.trim().ifBlank { null },
                genre = state.genre.trim().ifBlank { null },
                isCustom = state.isCustom
            )
            if (state.isEditing) repository.updateStation(station) else repository.addStation(station)
            _uiState.value = state.copy(isSaving = false)
            onSaved()
        }
    }

    companion object {
        fun factory(repository: StationRepository, stationId: String?): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return StationFormViewModel(repository, stationId) as T
                }
            }
    }
}
