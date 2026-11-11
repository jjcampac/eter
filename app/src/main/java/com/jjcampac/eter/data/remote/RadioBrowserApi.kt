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
