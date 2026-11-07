package com.jjcampac.eter.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Clave de preferencia para el modo de tema
private const val PREF_THEME_MODE = "pref_theme_mode"
private const val PREFS_NAME = "eter_prefs"

/**
 * Gestión de la preferencia de tema persistente.
 * Guarda la selección del usuario en SharedPreferences.
 */
class ThemePreferences(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Lee el tema guardado (por defecto SISTEMA). */
    fun getThemeMode(): ThemeMode {
        val valor = prefs.getString(PREF_THEME_MODE, null)
        return when (valor) {
            "CLARO" -> ThemeMode.CLARO
            "OSCURO" -> ThemeMode.OSCURO
            else -> ThemeMode.SISTEMA
        }
    }

    /** Guarda el tema seleccionado. */
    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(PREF_THEME_MODE, mode.name).apply()
    }
}

/** ViewModel para exponer ThemePreferences a la UI. */
class ThemeViewModel(private val context: Context) : ViewModel() {
    private val prefs = ThemePreferences(context)

    val themeMode: ThemeMode
        get() = prefs.getThemeMode()

    fun saveThemeMode(mode: ThemeMode) {
        prefs.saveThemeMode(mode)
    }

    companion object {
        fun factory(context: Context): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ThemeViewModel(context) as T
                }
            }
    }
}
