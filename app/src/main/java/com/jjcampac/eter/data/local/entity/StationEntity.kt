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
