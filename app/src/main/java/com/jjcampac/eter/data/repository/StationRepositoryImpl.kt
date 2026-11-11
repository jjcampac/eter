/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2026 jjcampac
 */

/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.data.repository

import android.util.Log
import com.google.gson.JsonParseException
import com.jjcampac.eter.data.local.dao.StationDao
import com.jjcampac.eter.data.local.entity.StationEntity
import com.jjcampac.eter.data.remote.RadioBrowserDataSource
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

/** Tag propio para el diagnóstico de la búsqueda en Radio Browser. */
private const val TAG = "RadioBrowserRepo"

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

    override suspend fun reorderFavorites(orderedIds: List<String>) =
        dao.reorderFavorites(orderedIds)

    override suspend fun nextFavoriteSortOrder(): Int = dao.nextSortOrder()

    /**
     * Búsqueda remota en Radio Browser.
     *
     * El `try/catch` solo sirve para diagnóstico: registra el error real con su
     * clase, mensaje, causa y stacktrace, y lo relanza tal cual. Así el
     * comportamiento y el mensaje que ve el usuario no cambian.
     */
    override suspend fun searchRemote(query: String, country: String, genre: String?): List<Station> {
        val source = remote ?: return emptyList()
        return try {
            source.searchStations(query, country, genre)
        } catch (e: Exception) {
            Log.e(TAG, describeSearchFailure(e), e)
            throw e
        }
    }

    /**
     * Clasifica el fallo de la búsqueda remota para que el log distinga entre
     * fallo de red, error HTTP y error de conversión/deserialización.
     */
    private fun describeSearchFailure(e: Exception): String {
        val detalle = buildString {
            append("clase=").append(e.javaClass.name)
            append(" mensaje=").append(e.message)
            val cause = e.cause
            if (cause != null) {
                append(" causa=").append(cause.javaClass.name).append(": ").append(cause.message)
            }
        }
        return when (e) {
            is HttpException -> buildString {
                append("ERROR HTTP codigo=").append(e.code()).append(" motivo=").append(e.message())
                val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                if (!cuerpo.isNullOrBlank()) {
                    append(" cuerpo=").append(cuerpo.take(300))
                }
                append(" | ").append(detalle)
            }
            is UnknownHostException ->
                "FALLO DE RED (DNS: host no resuelto) | $detalle"
            is SocketTimeoutException ->
                "FALLO DE RED (timeout) | $detalle"
            is SSLException ->
                "FALLO DE RED (TLS/SSL) | $detalle"
            is IOException ->
                "FALLO DE RED (IO, ${e.javaClass.simpleName}) | $detalle"
            is ClassCastException ->
                "ERROR DE CONVERSION (tipo no resoluble por Retrofit) | $detalle"
            is JsonParseException ->
                "ERROR DE DESERIALIZACION (JSON invalido) | $detalle"
            is IllegalArgumentException ->
                "ERROR DE CONVERSION/CONFIGURACION de Retrofit | $detalle"
            else ->
                "ERROR DESCONOCIDO | $detalle"
        }
    }
}

/** Mapeo de entidad Room a modelo de dominio. */
fun StationEntity.toDomain(): Station = Station(
    id = id,
    name = name,
    url = url,
    country = country,
    genre = genre,
    isFavorite = isFavorite,
    isCustom = isCustom,
    imageUrl = imageUrl,
    sortOrder = sortOrder
)

/** Mapeo de modelo de dominio a entidad Room. */
fun Station.toEntity(): StationEntity = StationEntity(
    id = id,
    name = name,
    url = url,
    country = country,
    genre = genre,
    isFavorite = isFavorite,
    isCustom = isCustom,
    imageUrl = imageUrl,
    sortOrder = sortOrder
)
