package com.jjcampac.eter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoritesFlow(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name ASC")
    suspend fun getFavorites(): List<StationEntity>
}
