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
package com.jjcampac.eter.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/**
 * ViewModel para exponer ThemePreferences a la UI.
 *
 * Mantiene un [StateFlow] con el tema actual para que la interfaz se actualice
 * al instante (sin reiniciar la app) cuando el usuario lo cambia en Ajustes.
 * Como MainActivity y SettingsScreen comparten la misma instancia de este
 * ViewModel (mismo ViewModelStoreOwner de la Activity), basta con observar el
 * flujo para recomponer `EterTheme` en vivo.
 */
class ThemeViewModel(private val context: Context) : ViewModel() {
    private val prefs = ThemePreferences(context)

    private val _themeMode = MutableStateFlow(prefs.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun saveThemeMode(mode: ThemeMode) {
        prefs.saveThemeMode(mode)
        _themeMode.value = mode
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
