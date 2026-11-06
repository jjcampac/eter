package com.jjcampac.eter.di

import android.content.Context
import com.jjcampac.eter.EterApplication
import com.jjcampac.eter.data.local.EterDatabase
import com.jjcampac.eter.data.repository.StationRepositoryImpl
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.player.RadioPlayer

/**
 * Contenedor de dependencias simple (sin Hilt).
 *
 * Delega en [EterApplication] para que el [RadioPlayer] sea una instancia
 * real de Application y no un @Volatile en un object (que se pierde al
 * morir el proceso).
 */
object AppModule {

    /** Devuelve el reproductor único de la app, creándolo la primera vez. */
    fun radioPlayer(context: Context): RadioPlayer {
        val appContext = context.applicationContext
        return if (appContext is EterApplication) {
            appContext.getRadioPlayer()
        } else {
            RadioPlayer(appContext)
        }
    }

    /** Devuelve el repositorio de emisoras (Room). */
    fun stationRepository(context: Context): StationRepository {
        val appContext = context.applicationContext
        return if (appContext is EterApplication) {
            appContext.getStationRepository()
        } else {
            StationRepositoryImpl(
                EterDatabase.getDatabase(appContext).stationDao()
            )
        }
    }
}
