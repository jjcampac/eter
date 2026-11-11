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
package com.jjcampac.eter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa una emisora de radio persistida.
 */
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val country: String? = null,
    val genre: String? = null,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    /** URL de la imagen corporativa (logo) de la emisora. */
    val imageUrl: String? = null,
    /** Orden de visualización en listas ordenables (favoritas). */
    val sortOrder: Int = 0
)
