package com.jjcampac.eter.data.repository

import com.jjcampac.eter.data.local.dao.StationDao
import com.jjcampac.eter.data.local.entity.StationEntity
import com.jjcampac.eter.data.remote.RadioBrowserDataSource
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación del repositorio de emisoras.
 *
 * Combina el almacenamiento local (Room, Fase 3) con la búsqueda remota en la
 * API de Radio Browser (Fase 4). Si no se inyecta fuente remota, las búsquedas
 * devuelven lista vacía, así la app sigue funcionando solo con emisoras locales.
 */
class StationRepositoryImpl(
    private val dao: StationDao,
    private val remote: RadioBrowserDataSource? = null
) : StationRepository {

    override fun getAllStations(): Flow<List<Station>> =
        dao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun getFavorites(): Flow<List<Station>> =
        dao.getFavoritesFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getStation(id: String): Station? =
        dao.getById(id)?.toDomain()

    override suspend fun addStation(station: Station) =
        dao.insert(station.toEntity())

    override suspend fun updateStation(station: Station) =
        dao.update(station.toEntity())

    override suspend fun deleteStation(station: Station) =
        dao.delete(station.toEntity())

    override suspend fun setFavorite(id: String, isFavorite: Boolean) =
        dao.setFavorite(id, isFavorite)

    override suspend fun getFavoritesList(): List<Station> =
        dao.getFavorites().map { it.toDomain() }

    override suspend fun searchRemote(query: String, country: String, genre: String?): List<Station> =
        remote?.searchStations(query, country, genre) ?: emptyList()
}

/** Mapeo de entidad Room a modelo de dominio. */
fun StationEntity.toDomain(): Station = Station(
    id = id,
    name = name,
    url = url,
    country = country,
    genre = genre,
    isFavorite = isFavorite,
    isCustom = isCustom
)

/** Mapeo de modelo de dominio a entidad Room. */
fun Station.toEntity(): StationEntity = StationEntity(
    id = id,
    name = name,
    url = url,
    country = country,
    genre = genre,
    isFavorite = isFavorite,
    isCustom = isCustom
)
