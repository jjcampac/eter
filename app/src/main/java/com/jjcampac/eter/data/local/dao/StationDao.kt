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
package com.jjcampac.eter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jjcampac.eter.data.local.entity.StationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD y favoritas sobre emisoras.
 */
@Dao
interface StationDao {

    @Query("SELECT * FROM stations ORDER BY name ASC")
    fun getAllFlow(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id = :id")
    suspend fun getById(id: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(station: StationEntity)

    @Update
    suspend fun update(station: StationEntity)

    @Delete
    suspend fun delete(station: StationEntity)

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY sortOrder ASC")
    fun getFavoritesFlow(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY sortOrder ASC")
    suspend fun getFavorites(): List<StationEntity>

    /** Actualiza el orden de visualización de una emisora favorita. */
    @Query("UPDATE stations SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    /**
     * Reordena todas las favoritas en una sola transacción, asignando sortOrder según la
     * lista de ids proporcionada (primero en la lista = sortOrder 0, etc.).
     */
    @Transaction
    suspend fun reorderFavorites(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }

    /** Devuelve el siguiente sortOrder disponible (max + 1, o 0 si no hay favoritas). */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM stations WHERE isFavorite = 1")
    suspend fun nextSortOrder(): Int
}
