/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter

import android.app.Application
import android.content.SharedPreferences
import com.jjcampac.eter.data.local.EterDatabase
import com.jjcampac.eter.data.local.dao.StationDao
import com.jjcampac.eter.data.remote.RadioBrowserDataSource
import com.jjcampac.eter.data.repository.StationRepositoryImpl
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.player.RadioPlayer
import com.jjcampac.eter.player.SleepTimer
import com.jjcampac.eter.util.AppLanguage

/**
 * Clase Application de Eter.
 *
 * Mantiene el [RadioPlayer] como campo de instancia para que la UI y el
 * [com.jjcampac.eter.player.service.RadioMediaSessionService] compartan
 * la misma instancia de ExoPlayer incluso cuando la actividad se recrea.
 *
 * Además persiste la URL de la última emisora en SharedPreferences para
 * poder auto-reanudar la reproducción si el proceso fue muerto por el
 * sistema (pantalla apagada, app en segundo plano, etc.).
 */
class EterApplication : Application() {

    private lateinit var _radioPlayer: RadioPlayer

    override fun onCreate() {
        super.onCreate()
        // Cargar el idioma guardado en el flujo observado por la UI.
        AppLanguage.init(applicationContext)
    }

    /** Devuelve el reproductor único, creándolo si es necesario. */
    fun getRadioPlayer(): RadioPlayer {
        if (!::_radioPlayer.isInitialized) {
            _radioPlayer = RadioPlayer(applicationContext)
        }
        return _radioPlayer
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
}