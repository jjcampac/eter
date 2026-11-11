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
package com.jjcampac.eter.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Emisora devuelta por la API de Radio Browser (Advanced Station Search).
 *
 * Solo contiene los campos que la app necesita para construir un [com.jjcampac.eter.domain.model.Station].
 * Los nombres serializados por Gson se declaran con [SerializedName] porque la API usa snake_case.
 */
data class StationDto(
    @SerializedName("stationuuid")
    val stationuuid: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("url")
    val url: String?,
    /** URL resuelta por Radio Browser (evita enlaces intermedios rotos). */
    @SerializedName("url_resolved")
    val urlResolved: String?,
    @SerializedName("country")
    val country: String?,
    /** Géneros separados por comas (pop, rock, news...). */
    @SerializedName("tags")
    val tags: String?,
    /** URL del favicon/logo de la emisora (imagen corporativa). */
    @SerializedName("favicon")
    val favicon: String?,
    /** URL del logo en mayor resolución (si la emisora lo aporta). */
    @SerializedName("logo_300x300")
    val logo300x300: String?
)
