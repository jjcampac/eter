/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.domain.model

/**
 * Modelo de dominio de una emisora de radio.
 *
 * @param id         Identificador único. Para emisoras del catálogo de Radio Browser
 *                   será el `stationuuid`; para emisoras manuales un UUID local.
 * @param url        URL del stream (http o https).
 * @param isCustom   `true` si fue añadida manualmente por el usuario.
 */
data class Station(
    val id: String,
    val name: String,
    val url: String,
    val country: String? = null,
    val genre: String? = null,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
) {
    /** Etiqueta secundaria mostrada en las listas (país · género). */
    val subtitle: String
        get() {
            val parts = listOfNotNull(country, genre)
            return if (parts.isEmpty()) "" else parts.joinToString(" · ")
        }
}
