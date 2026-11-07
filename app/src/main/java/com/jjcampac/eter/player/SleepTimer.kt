package com.jjcampac.eter.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.jjcampac.eter.player.service.ACTION_SLEEP_TIMER
import com.jjcampac.eter.player.service.RadioMediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "SleepTimer"

/**
 * Temporizador para dormir (Fase 5).
 *
 * Cuenta atrás mostrando el tiempo restante en la pantalla del reproductor.
 * Al agotarse, pausa la reproducción y retira la notificación enviando la
 * acción [ACTION_SLEEP_TIMER] al servicio de medios (la aplicación no se
 * cierra). La selección de minutos se persiste para que Ajustes muestre la
 * opción elegida al reabrir la pantalla.
 */
class SleepTimer(context: Context) {

    /** Estado observable por la UI (si está activo y tiempo restante). */
    data class State(
        val active: Boolean = false,
        val remainingMillis: Long = 0L
    )

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var countdownJob: Job? = null
    private var endElapsedMs = 0L

    /** Minutos elegidos en Ajustes (0 = desactivado), persistidos en prefs. */
    fun selectedMinutes(): Int = prefs.getInt(PREF_SLEEP_MINUTES, UNSELECTED)

    /** Inicia (o reinicia) el temporizador con `minutes` minutos. */
    fun start(minutes: Int) {
        stop()
        if (minutes <= 0) return
        prefs.edit().putInt(PREF_SLEEP_MINUTES, minutes).apply()
        endElapsedMs = SystemClock.elapsedRealtime() + minutes * 60_000L
        _state.value = State(active = true, remainingMillis = minutes * 60_000L)
        Log.d(TAG, "start($minutes min)")
        countdownJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val remaining = endElapsedMs - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    Log.d(TAG, "Tiempo agotado: apagando la reproducción.")
                    _state.value = State(active = false, remainingMillis = 0L)
                    prefs.edit().remove(PREF_SLEEP_MINUTES).apply()
                    notifyExpired()
                    break
                }
                _state.value = State(active = true, remainingMillis = remaining)
            }
        }
    }

    /** Cancela el temporizador (Ajustes → "Desactivado"). */
    fun stop() {
        countdownJob?.cancel()
        countdownJob = null
        prefs.edit().remove(PREF_SLEEP_MINUTES).apply()
        _state.value = State(active = false, remainingMillis = 0L)
    }

    /** Pide al servicio de medios que pause y quite la notificación. */
    private fun notifyExpired() {
        val intent = Intent(appContext, RadioMediaSessionService::class.java)
            .setAction(ACTION_SLEEP_TIMER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    private companion object {
        const val PREFS_NAME = "eter_prefs"
        const val PREF_SLEEP_MINUTES = "pref_sleep_minutes"
        const val UNSELECTED = 0
        const val TICK_MS = 1_000L
    }
}
