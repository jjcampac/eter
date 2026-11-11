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
    val isCustom: Boolean = false,
    /** URL de la imagen corporativa (logo) de la emisora, si la aporta la fuente. */
    val imageUrl: String? = null,
    /** Orden de visualización en listas ordenables (favoritas). */
    val sortOrder: Int = 0
) {
    /** Etiqueta secundaria mostrada en las listas (país · género). */
    val subtitle: String
        get() {
            val parts = listOfNotNull(country, genre)
            return if (parts.isEmpty()) "" else parts.joinToString(" · ")
        }
}
