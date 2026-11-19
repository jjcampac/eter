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
package com.jjcampac.eter.player.audio

import android.util.Log
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

private const val TAG = "AudioSpectrum"

/**
 * Puente entre el audio que se está reproduciendo y la interfaz.
 *
 * El [EspectroAudioProcessor] (que vive en el hilo del reproductor, dentro del
 * [androidx.media3.exoplayer.audio.DefaultAudioSink]) publica aquí los niveles
 * de las bandas y la UI los observa con un [StateFlow]. Así el reproductor y
 * Compose se comunican sin conocerse ni compartir referencias.
 *
 * Los valores son 6 bandas (graves → agudos) ya normalizadas entre 0 y 1.
 */
object AudioSpectrum {

    /** Número de bandas del espectro (graves a agudos). */
    const val BANDAS = 6

    /** Caída por paso del desvanecido (≈18 % cada 16 ms, como el suavizado de la UI). */
    private const val CAIDA_POR_PASO = 0.82f

    /** Intervalo entre pasos del desvanecido. */
    private const val PASO_MS = 16L

    /** Por debajo de este valor se considera silencio y se termina el desvanecido. */
    private const val UMBRAL_SILENCIO = 0.01f

    private val _niveles = MutableStateFlow(FloatArray(BANDAS))

    /** Niveles actuales del espectro (solo lectura, para la UI). */
    val niveles: StateFlow<FloatArray> = _niveles.asStateFlow()

    /** Ámbito propio para el desvanecido; no depende del ciclo de vida de la UI. */
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var desvanecido: Job? = null

    /**
     * Publica una medida nueva del analizador.
     *
     * Se copia el array porque el analizador reutiliza siempre el mismo buffer
     * (no se asignan buffers de audio por bloque; esta copia son 6 floats).
     */
    @Synchronized
    fun publicar(valores: FloatArray) {
        if (_niveles.value.contentEquals(valores)) return
        desvanecido?.cancel()
        desvanecido = null
        _niveles.value = valores.copyOf()
    }

    /**
     * Baja los niveles hasta cero con suavidad (pausa, parada, cambio de emisora
     * o flush/reset del audio). La UI sigue dibujando durante la caída.
     */
    @Synchronized
    fun silenciar() {
        desvanecido?.cancel()
        desvanecido = ambito.launch {
            var actuales = _niveles.value.copyOf()
            while (isActive && actuales.any { it > UMBRAL_SILENCIO }) {
                val siguientes = FloatArray(BANDAS) { actuales[it] * CAIDA_POR_PASO }
                _niveles.value = siguientes
                actuales = siguientes
                delay(PASO_MS)
            }
            _niveles.value = FloatArray(BANDAS)
        }
    }

    /** Pone los niveles a cero sin animación (al liberar el reproductor). */
    @Synchronized
    fun reiniciar() {
        desvanecido?.cancel()
        desvanecido = null
        if (_niveles.value.any { it != 0f }) {
            Log.d(TAG, "reiniciar(): niveles a cero")
        }
        _niveles.value = FloatArray(BANDAS)
    }
}
