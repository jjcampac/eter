package com.jjcampac.eter.domain.repository

import com.jjcampac.eter.domain.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de emisoras.
 */
interface StationRepository {
    /** Todas las emisoras, ordenadas por nombre. */
    fun getAllStations(): Flow<List<Station>>

    /** Solo las favoritas, ordenadas por nombre. */
    fun getFavorites(): Flow<List<Station>>

    /** Obtiene una emisora por id. */
    suspend fun getStation(id: String): Station?

    /** Inserta o reemplaza una emisora. */
    suspend fun addStation(station: Station)

    /** Actualiza una emisora existente. */
    suspend fun updateStation(station: Station)

    /** Elimina una emisora. */
    suspend fun deleteStation(station: Station)

    /** Marca o desmarca una emisora como favorita. */
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /** Lista síncrona de favoritas (para navegación anterior/siguiente). */
    suspend fun getFavoritesList(): List<Station>
}
