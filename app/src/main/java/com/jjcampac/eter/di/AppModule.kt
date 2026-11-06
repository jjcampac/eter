package com.jjcampac.eter.di

import android.content.Context
import com.jjcampac.eter.player.RadioPlayer

/**
 * Contenedor de dependencias simple (sin Hilt).
 *
 * Se usa un singleton por proceso para que la UI y el `MediaSessionService`
 * compartan la misma instancia de [RadioPlayer].
 */
object AppModule {

    @Volatile
    private var radioPlayer: RadioPlayer? = null

    /** Devuelve el reproductor único de la app, creándolo la primera vez. */
    fun radioPlayer(context: Context): RadioPlayer =
        radioPlayer ?: synchronized(this) {
            radioPlayer ?: RadioPlayer(context.applicationContext).also { radioPlayer = it }
        }
}
