package com.jjcampac.eter.data.remote

import com.jjcampac.eter.data.remote.dto.ServerDto
import com.jjcampac.eter.data.remote.dto.StationDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfaz Retrofit de la API pública de Radio Browser.
 *
 * La base URL se resuelve dinámicamente a un mirror (ver [RadioBrowserDataSource]).
 * Toda petición se hace con el User-Agent propio de la app, tal y como exige la
 * API para ser identificada.
 */
interface RadioBrowserApi {

    /** Lista de mirrors disponibles para elegir uno aleatorio. */
    @GET("json/servers")
    suspend fun servers(): List<ServerDto>

    /** Búsqueda avanzada de emisoras (nombre, país y género). */
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String?,
        @Query("countrycode") countryCode: String?,
        @Query("tag") tag: String?,
        @Query("limit") limit: Int = 50,
        @Query("hidebroken") hidebroken: Boolean = true,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true
    ): List<StationDto>
}
