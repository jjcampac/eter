package com.jjcampac.eter.util

import android.content.Context
import android.content.SharedPreferences

// Misma SharedPreferences que usan el resto de ajustes de la app.
private const val PREFS_NAME = "eter_prefs"
private const val PREF_RECONNECT_SECONDS = "pref_reconnect_seconds"

/**
 * Preferencia de reconexión: cuántos segundos se reintenta conectar con una
 * emisora antes de avisar al usuario (Fase 5). Valor 0 = sin límite
 * (reintentar siempre, como se hacía antes de Fase 5).
 *
 * Se guarda en SharedPreferences y [com.jjcampac.eter.player.RadioPlayer] lo
 * lee en el momento del fallo, por lo que los cambios en Ajustes se aplican
 * inmediatamente al siguiente corte.
 */
class ReconnectPreferences(context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Segundos de ventana de reintento (0 = sin límite). Por defecto 20. */
    fun getReconnectSeconds(): Int =
        prefs.getInt(PREF_RECONNECT_SECONDS, DEFAULT_RECONNECT_SECONDS)

    fun saveReconnectSeconds(seconds: Int) {
        prefs.edit().putInt(PREF_RECONNECT_SECONDS, seconds).apply()
    }

    companion object {
        const val DEFAULT_RECONNECT_SECONDS = 20
        const val UNLIMITED = 0
    }
}
