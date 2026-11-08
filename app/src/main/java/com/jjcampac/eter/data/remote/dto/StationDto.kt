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
    val tags: String?
)
