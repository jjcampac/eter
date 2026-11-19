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

import android.os.SystemClock
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "AnalizadorEspectro"

/**
 * Analizador de espectro ligero, sin dependencias externas.
 *
 * - Mezcla el PCM a mono y lo acumula en una ventana de [TAMANO_FFT] muestras
 *   con ventana de Hann (solape del 50 % para que el movimiento sea continuo).
 * - Calcula la FFT con un Cooley-Tukey radix-2 propio, con tablas precalculadas.
 * - Agrupa el espectro en [AudioSpectrum.BANDAS] bandas logarítmicas, las
 *   convierte a dB y las normaliza entre 0 y 1.
 *
 * Todos los buffers se crean una vez y se reutilizan: no se asigna memoria al
 * procesar cada bloque de audio. Como mucho se analiza cada
 * [MS_ENTRE_ANALISIS] ms (~30 veces por segundo).
 */
internal class AnalizadorEspectro {

    /** Formato configurado actualmente (si no, `analizar` no hace nada). */
    private var configurado = false
    private var esFloat = false
    private var canales = 2
    private var frecuenciaMuestreo = 44_100

    // Acumulador mono de la ventana actual.
    private val mono = FloatArray(TAMANO_FFT)
    private var escritas = 0

    // Buffers y tablas de la FFT (reutilizados en cada análisis).
    private val real = FloatArray(TAMANO_FFT)
    private val imag = FloatArray(TAMANO_FFT)
    private val ventana = FloatArray(TAMANO_FFT)
    private val inversion = IntArray(TAMANO_FFT)
    private val coseno = FloatArray(TAMANO_FFT / 2)
    private val seno = FloatArray(TAMANO_FFT / 2)

    // Bins de cada banda (inicio incluido, fin excluido).
    private val inicioBanda = IntArray(AudioSpectrum.BANDAS)
    private val finBanda = IntArray(AudioSpectrum.BANDAS)

    /** Niveles normalizados de la última medida; se leen desde el procesador. */
    val niveles = FloatArray(AudioSpectrum.BANDAS)

    private var ultimoAnalisisMs = 0L

    /**
     * Configura el analizador para un formato PCM concreto. Solo se admiten
     * 16 bits y float (el procesador descarta el resto antes de llegar aquí).
     */
    fun configurar(frecuenciaMuestreo: Int, canales: Int, esFloat: Boolean) {
        this.frecuenciaMuestreo = frecuenciaMuestreo
        this.canales = canales.coerceAtLeast(1)
        this.esFloat = esFloat
        reiniciar()
        prepararTablas()
        prepararBandas()
        configurado = true
        Log.d(TAG, "configurado: ${frecuenciaMuestreo} Hz, ${this.canales} canal(es), float=$esFloat")
    }

    /** Descarta la ventana en curso y deja los niveles a cero (flush/reset). */
    fun reiniciar() {
        escritas = 0
        mono.fill(0f)
        niveles.fill(0f)
        ultimoAnalisisMs = 0L
    }

    /**
     * Analiza el PCM contenido en [buffer] sin consumirlo ni modificar su
     * posición. Devuelve `true` si hay una medida nueva en [niveles].
     */
    fun analizar(buffer: ByteBuffer): Boolean {
        if (!configurado) return false

        val bytesPorMuestra = if (esFloat) 4 else 2
        val bytesPorBloque = bytesPorMuestra * canales
        val desde = buffer.position()
        val bloques = (buffer.limit() - desde) / bytesPorBloque
        if (bloques <= 0) return false

        // El PCM decodificado por Media3 es siempre little-endian.
        val ordenOriginal = buffer.order()
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        var publicado = false
        for (f in 0 until bloques) {
            val base = desde + f * bytesPorBloque
            var suma = 0f
            for (c in 0 until canales) {
                suma += if (esFloat) {
                    buffer.getFloat(base + c * 4)
                } else {
                    buffer.getShort(base + c * 2) / 32768f
                }
            }
            // Mezcla a mono para el análisis (sin tocar el audio original).
            mono[escritas++] = suma / canales
            if (escritas == TAMANO_FFT) {
                val ahora = SystemClock.elapsedRealtime()
                // La primera medida tras un reset es inmediata; después, ~30/s.
                if (ultimoAnalisisMs == 0L || ahora - ultimoAnalisisMs >= MS_ENTRE_ANALISIS) {
                    ultimoAnalisisMs = ahora
                    calcularEspectro()
                    publicado = true
                }
                // Se avanza media ventana: solape del 50 %.
                System.arraycopy(mono, TAMANO_FFT / 2, mono, 0, TAMANO_FFT / 2)
                escritas = TAMANO_FFT / 2
            }
        }
        buffer.order(ordenOriginal)
        return publicado
    }

    /** Precalcula ventana de Hann, permutación de bits y twiddles de la FFT. */
    private fun prepararTablas() {
        val n = TAMANO_FFT
        for (i in 0 until n) {
            ventana[i] = (0.5 - 0.5 * cos(2.0 * PI * i / (n - 1))).toFloat()
        }
        var bits = 0
        while ((1 shl bits) < n) bits++
        for (i in 0 until n) {
            var invertido = 0
            for (b in 0 until bits) {
                if ((i shr b) and 1 == 1) invertido = invertido or (1 shl (bits - 1 - b))
            }
            inversion[i] = invertido
        }
        for (k in 0 until n / 2) {
            coseno[k] = cos(2.0 * PI * k / n).toFloat()
            seno[k] = sin(2.0 * PI * k / n).toFloat()
        }
    }

    /** Calcula los límites (en bins) de las bandas logarítmicas. */
    private fun prepararBandas() {
        val n = TAMANO_FFT
        val hzPorBin = frecuenciaMuestreo.toFloat() / n
        // Sin pasar del límite de Nyquist para no leer bins sin información.
        val frecuenciaMaxima = min(16_000f, frecuenciaMuestreo * 0.45f)
        var bordeInferior = FRECUENCIA_MINIMA_HZ
        for (b in 0 until AudioSpectrum.BANDAS) {
            val proporcion = (b + 1).toFloat() / AudioSpectrum.BANDAS
            val bordeSuperior = FRECUENCIA_MINIMA_HZ *
                (frecuenciaMaxima / FRECUENCIA_MINIMA_HZ).pow(proporcion)
            val desde = (bordeInferior / hzPorBin).toInt().coerceIn(1, n / 2 - 1)
            val hasta = (bordeSuperior / hzPorBin).toInt().coerceIn(desde + 1, n / 2)
            inicioBanda[b] = desde
            finBanda[b] = hasta
            bordeInferior = bordeSuperior
        }
    }

    /** FFT radix-2 en el sitio y magnitudes por banda. */
    private fun calcularEspectro() {
        val n = TAMANO_FFT
        for (i in 0 until n) {
            real[i] = mono[i] * ventana[i]
            imag[i] = 0f
        }
        // Reordenación por inversión de bits.
        for (i in 0 until n) {
            val j = inversion[i]
            if (j > i) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }
        // Mariposas de Cooley-Tukey.
        var tamano = 2
        while (tamano <= n) {
            val mitad = tamano / 2
            val paso = n / tamano
            var inicio = 0
            while (inicio < n) {
                var k = 0
                for (i in inicio until inicio + mitad) {
                    val j = i + mitad
                    val wr = coseno[k]
                    val wi = -seno[k]
                    val tr = real[j] * wr - imag[j] * wi
                    val ti = real[j] * wi + imag[j] * wr
                    real[j] = real[i] - tr
                    imag[j] = imag[i] - ti
                    real[i] += tr
                    imag[i] += ti
                    k += paso
                }
                inicio += tamano
            }
            tamano *= 2
        }
        // 4/n: amplitud de un tono con ventana de Hann (doble lado y coherencia).
        val ganancia = 4f / n
        for (b in 0 until AudioSpectrum.BANDAS) {
            var pico = 0f
            for (k in inicioBanda[b] until finBanda[b]) {
                val magnitud = sqrt(real[k] * real[k] + imag[k] * imag[k]) * ganancia
                if (magnitud > pico) pico = magnitud
            }
            niveles[b] = normalizar(pico)
        }
    }

    /** Amplitud lineal → nivel 0..1 con escala en decibelios. */
    private fun normalizar(amplitud: Float): Float {
        if (amplitud <= 0f) return 0f
        val db = 20f * log10(amplitud.coerceAtLeast(1e-6f))
        val proporcion = ((db - DB_MINIMO) / (DB_MAXIMO - DB_MINIMO)).coerceIn(0f, 1f)
        return proporcion.pow(GAMMA)
    }

    private companion object {
        /** Muestras por ventana de análisis (potencia de 2). */
        const val TAMANO_FFT = 1024

        /** Mínimo entre análisis: ~30 emisiones por segundo. */
        const val MS_ENTRE_ANALISIS = 33L

        /** Frecuencia del primer borde de banda. */
        const val FRECUENCIA_MINIMA_HZ = 40f

        /** Rango dinámico que se mapea a 0..1. */
        const val DB_MINIMO = -70f
        const val DB_MAXIMO = -3f

        /** Curva aplicada para dar más contraste visual. */
        const val GAMMA = 1.5f
    }
}
