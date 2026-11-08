/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jjcampac.eter.data.local.dao.StationDao
import com.jjcampac.eter.data.local.entity.StationEntity

/**
 * Base de datos Room de la aplicación (emisoras y favoritas).
 */
@Database(entities = [StationEntity::class], version = 2, exportSchema = false)
abstract class EterDatabase : RoomDatabase() {

    abstract fun stationDao(): StationDao

    companion object {
        @Volatile
        private var INSTANCE: EterDatabase? = null

        /** v1 → v2: añade la columna `imageUrl` (logo corporativo de la emisora). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stations ADD COLUMN imageUrl TEXT")
            }
        }

        fun getDatabase(context: Context): EterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EterDatabase::class.java,
                    "eter_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
