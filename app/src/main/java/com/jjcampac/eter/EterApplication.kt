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
package com.jjcampac.eter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.jjcampac.eter.data.local.EterDatabase
import com.jjcampac.eter.data.local.dao.StationDao
import com.jjcampac.eter.data.remote.RadioBrowserDataSource
import com.jjcampac.eter.data.repository.StationRepositoryImpl
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.player.SleepTimer
import com.jjcampac.eter.util.AppLanguage
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

/**
 * Clase Application de Eter.
 *
 * El reproductor pertenece en exclusiva a
 * [com.jjcampac.eter.player.service.RadioMediaSessionService]; la Application
 * solo guarda el estado persistente y las dependencias compartidas.
 *
 * Además persiste la URL de la última emisora en SharedPreferences para
 * poder auto-reanudar la reproducción si el proceso fue muerto por el
 * sistema (pantalla apagada, app en segundo plano, etc.).
 */
class EterApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Cargar el idioma guardado en el flujo observado por la UI.
        AppLanguage.init(applicationContext)
    }

    /**
     * Configura el ImageLoader único de Coil 3 (ses carga la primera vez que se usa).
     *
     * - Caché de memoria y de disco (límite 50 MB cada una), de modo que los iconos
     *   de emisora ya cargados se siguen viendo sin conexión (Paso b).
     * - Red a través de OkHttp (coil-network-okhttp) reutilizando la misma versión de
     *   OkHttp del proyecto, con el User-Agent de la app, sin cookies y solo peticiones
     *   GET: se mantiene la política de privacidad (sin cabeceras de identificación
     *   adicionales ni seguimiento).
     */
    override fun newImageLoader(context: Context): ImageLoader {
        // Cliente OkHttp de imágenes: solo User-Agent de la app. OkHttp no añade cookies
        // si no se registra un CookieJar, así que no se guarda ni envía ninguna.
        val clienteImagenes = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                )
            }
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { clienteImagenes }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(MEMORY_CACHE_BYTES)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(IMAGE_CACHE_DIR).toOkioPath())
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .build()
    }

    private val database: EterDatabase by lazy {
        EterDatabase.getDatabase(applicationContext)
    }

    private val _stationRepository: StationRepository by lazy {
        StationRepositoryImpl(database.stationDao(), RadioBrowserDataSource())
    }

    private val _sleepTimer: SleepTimer by lazy {
        SleepTimer(applicationContext)
    }

    /** Devuelve el temporizador para dormir compartido (Fase 5). */
    fun getSleepTimer(): SleepTimer = _sleepTimer

    /** Devuelve el repositorio de emisoras (Room). */
    fun getStationRepository(): StationRepository = _stationRepository

    private fun getPrefs(): SharedPreferences =
        getSharedPreferences("eter_prefs", MODE_PRIVATE)

    /** Guarda la URL de la última emisora reproducida. */
    fun saveLastStationUrl(url: String) {
        getPrefs().edit().putString("last_station_url", url).apply()
    }

    /** Devuelve la URL de la última emisora, o null si no hay ninguna guardada. */
    fun getLastStationUrl(): String? =
        getPrefs().getString("last_station_url", null)

    companion object {
        private const val USER_AGENT = "Eter/1.0 (jjcapa@gmail.com)"
        private const val IMAGE_CACHE_DIR = "eter_image_cache"
        private const val DISK_CACHE_BYTES = 50L * 1024 * 1024
        private const val MEMORY_CACHE_BYTES = 50L * 1024 * 1024
    }
}