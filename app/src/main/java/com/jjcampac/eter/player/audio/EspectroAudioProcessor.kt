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
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

private const val TAG = "EspectroAudioProcessor"

/**
 * Procesador de audio que analiza lo que se está reproduciendo y lo publica en
 * [AudioSpectrum] para el visualizador de la UI.
 *
 * El audio **no se modifica**: el formato de salida es el de entrada y el bloque
 * de entrada se copia tal cual al de salida (sin procesado, sin latencia y sin
 * cortes). Solo se analizan copias en mono de las muestras.
 *
 * Se soportan PCM 16 bits y PCM float. Con cualquier otro formato el procesador
 * queda inactivo (Media3 lo salta) y el audio sigue su curso intacto.
 */
@UnstableApi
class EspectroAudioProcessor : BaseAudioProcessor() {

    private val analizador = AnalizadorEspectro()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val soportado = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        if (!soportado) {
            // Passthrough total: sin análisis y sin tocar el audio.
            Log.d(TAG, "Formato no soportado (${inputAudioFormat.encoding}): se deja pasar")
            AudioSpectrum.silenciar()
            return AudioProcessor.AudioFormat.NOT_SET
        }
        analizador.configurar(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        )
        Log.d(
            TAG,
            "Formato de entrada: ${inputAudioFormat.sampleRate} Hz, " +
                "${inputAudioFormat.channelCount} canal(es), encoding=${inputAudioFormat.encoding}"
        )
        // Mismo formato a la salida: el sonido no se altera.
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        // Media3 también llama con un buffer vacío (AudioProcessor.EMPTY_BUFFER)
        // cuando el pipeline se queda sin datos. En ese caso no hay nada que
        // analizar ni copiar: y no se debe tocar el buffer interno, porque
        // `replaceOutputBuffer(0)` devolvería ese mismo objeto vacío y copiarlo
        // sobre sí mismo lanza IllegalArgumentException.
        if (bytes == 0) return

        // 1) Análisis (no consume ni altera el buffer de entrada).
        if (analizador.analizar(inputBuffer)) {
            AudioSpectrum.publicar(analizador.niveles)
        }
        // 2) Paso directo: se copia el bloque tal cual (el sonido no se toca).
        val salida = replaceOutputBuffer(bytes)
        salida.put(inputBuffer)
        salida.flip()
    }

    override fun onFlush() {
        analizador.reiniciar()
        AudioSpectrum.silenciar()
    }

    override fun onReset() {
        analizador.reiniciar()
        AudioSpectrum.silenciar()
    }
}
