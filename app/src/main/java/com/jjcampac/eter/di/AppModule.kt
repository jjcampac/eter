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
package com.jjcampac.eter.di

import android.content.Context
import com.jjcampac.eter.EterApplication
import com.jjcampac.eter.data.local.EterDatabase
import com.jjcampac.eter.data.remote.RadioBrowserDataSource
import com.jjcampac.eter.data.repository.StationRepositoryImpl
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.player.SleepTimer

/**
 * Contenedor de dependencias simple (sin Hilt).
 *
 * El reproductor no se expone aquí: su único dueño es el servicio de medios.
 * Este contenedor solo entrega las dependencias compartidas de la Application.
 */
object AppModule {

    /** Devuelve el temporizador para dormir compartido (Fase 5). */
    fun sleepTimer(context: Context): SleepTimer {
        val appContext = context.applicationContext
        return if (appContext is EterApplication) {
            appContext.getSleepTimer()
        } else {
            SleepTimer(appContext)
        }
    }

    /** Devuelve el repositorio de emisoras (Room). */
    fun stationRepository(context: Context): StationRepository {
        val appContext = context.applicationContext
        return if (appContext is EterApplication) {
            appContext.getStationRepository()
        } else {
            StationRepositoryImpl(
                EterDatabase.getDatabase(appContext).stationDao(),
                RadioBrowserDataSource()
            )
        }
    }
}
