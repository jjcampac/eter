/*
 * Autor: jjcampac
 * IA: cline
 */
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

    /**
     * Busca emisoras en la API pública de Radio Browser.
     * Devuelve lista vacía si el repositorio no tiene fuente remota configurada.
     *
     * @param query   Término a buscar en el nombre de la emisora.
     * @param country País para filtrar (España por defecto en la UI).
     * @param genre   Género/etiqueta opcional (nulo = sin filtro).
     */
    suspend fun searchRemote(query: String, country: String, genre: String? = null): List<Station>
}
