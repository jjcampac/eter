package com.jjcampac.eter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jjcampac.eter.data.local.dao.StationDao
import com.jjcampac.eter.data.local.entity.StationEntity

/**
 * Base de datos Room de la aplicación (emisoras y favoritas).
 */
@Database(entities = [StationEntity::class], version = 1, exportSchema = false)
abstract class EterDatabase : RoomDatabase() {

    abstract fun stationDao(): StationDao

    companion object {
        @Volatile
        private var INSTANCE: EterDatabase? = null

        fun getDatabase(context: Context): EterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EterDatabase::class.java,
                    "eter_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
