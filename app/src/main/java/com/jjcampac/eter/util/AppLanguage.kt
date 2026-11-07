package com.jjcampac.eter.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "eter_prefs"
private const val PREF_LANGUAGE = "pref_language"

/** Opción de idioma ofrecida en Ajustes. */
data class LanguageOption(
    val code: String,
    val name: String
)

/**
 * Gestión del idioma de la interfaz (Fase 5).
 *
 * El idioma se guarda en SharedPreferences y se aplica en `attachBaseContext`
 * de la Activity envolviendo el contexto con `createConfigurationContext`.
 * Al cambiar de idioma en Ajustes se reinicia/recrea la Activity para que todos
 * los `stringResource` se resuelvan en el nuevo idioma al instante.
 */
object AppLanguage {

    const val DEFAULT = "es"

    /** Idioma actual de la UI, observable para que las pantallas reaccionen en vivo. */
    private val _currentLanguage = MutableStateFlow(DEFAULT)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    /** Idiomas disponibles (nombre en su propio idioma). */
    val options = listOf(
        LanguageOption("es", "Español"),
        LanguageOption("en", "English"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("fr", "Français"),
        LanguageOption("it", "Italiano"),
        LanguageOption("ja", "日本語")
    )

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Código de idioma actual (ISO 639-1, por defecto "es"). */
    fun current(context: Context): String =
        prefs(context).getString(PREF_LANGUAGE, DEFAULT) ?: DEFAULT

    /** Carga en el [currentLanguage] el idioma guardado (llamar en Application.onCreate). */
    fun init(context: Context) {
        _currentLanguage.value = current(context)
    }

    fun save(context: Context, code: String) {
        prefs(context).edit().putString(PREF_LANGUAGE, code).apply()
        _currentLanguage.value = code
    }

    /**
     * País por defecto de la búsqueda asociado a cada idioma (ISO 3166-1 alpha-2).
     * Ej.: es → España (ES), de → Alemania (DE), ja → Japón (JP).
     */
    fun defaultCountryIso(uiLanguageCode: String): String = when (uiLanguageCode) {
        "en" -> "US"
        "de" -> "DE"
        "fr" -> "FR"
        "it" -> "IT"
        "ja" -> "JP"
        else -> "ES"
    }

    /**
     * Nombre de ese país por defecto, mostrado en el idioma de la interfaz
     * (Ej. en alemán "Deutschland", en japonés "日本", en español "España").
     */
    fun defaultCountryDisplayName(uiLanguageCode: String): String {
        val iso = defaultCountryIso(uiLanguageCode)
        val uiLocale = Locale.forLanguageTag(uiLanguageCode)
        val countryLocale = Locale.Builder()
            .setLanguage(uiLanguageCode)
            .setRegion(iso)
            .build()
        return countryLocale.getDisplayCountry(uiLocale)
    }

    /**
     * Envuelve [base] para que sus recursos se resuelvan en el idioma guardado.
     * Se llama desde `attachBaseContext` de la Activity.
     */
    fun wrapWithAppLocale(base: Context): Context {
        val locale = Locale.forLanguageTag(current(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
