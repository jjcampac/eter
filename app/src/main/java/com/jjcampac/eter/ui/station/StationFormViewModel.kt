/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.station

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "StationFormVM"

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

    private val _uiState = MutableStateFlow(
        StationFormUiState(isEditing = stationId != null, id = stationId)
    )
    val uiState: StateFlow<StationFormUiState> = _uiState.asStateFlow()

    init {
        if (stationId != null) loadStation(stationId)
    }

    /**
     * Prepara el formulario para una nueva apertura.
     *
     * Se llama cada vez que se abre (FAB o edición), porque el ViewModel sobrevive
     * al cierre del formulario: sin este reinicio se arrastrarían los datos de la
     * apertura anterior. Con `null` deja el alta en blanco; con un id lo limpia y
     * carga después los datos de la emisora a editar.
     */
    fun open(stationId: String?) {
        _uiState.value = StationFormUiState(isEditing = stationId != null, id = stationId)
        if (stationId != null) loadStation(stationId)
    }

    /** Carga en el formulario los datos de la emisora que se va a editar. */
    private fun loadStation(id: String) {
        viewModelScope.launch {
            val station = repository.getStation(id) ?: return@launch
            // Si mientras se leía la emisora se abrió otro formulario, se descarta.
            if (_uiState.value.id != station.id) return@launch
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
            _uiState.value = _uiState.value.copy(isSaving = true)
            val station = Station(
                id = state.id ?: UUID.randomUUID().toString(),
                name = state.name.trim(),
                url = state.url.trim(),
                country = state.country.trim().ifBlank { null },
                genre = state.genre.trim().ifBlank { null },
                isCustom = state.isCustom
            )
            try {
                if (state.isEditing) repository.updateStation(station) else repository.addStation(station)
                _uiState.value = _uiState.value.copy(isSaving = false)
                // Alta/edición correcta: se cierra el formulario.
                onSaved()
            } catch (e: Exception) {
                // Si el guardado falla no se abandona el formulario, pero se quita
                // el estado "guardando" para no dejarlo bloqueado y se informa.
                Log.e(TAG, "save() error=${e.javaClass.name}: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "No se pudo guardar la emisora. Inténtalo de nuevo."
                )
            }
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
