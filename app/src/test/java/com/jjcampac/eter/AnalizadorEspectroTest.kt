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
package com.jjcampac.eter

import com.jjcampac.eter.player.audio.AnalizadorEspectro
import com.jjcampac.eter.player.audio.AudioSpectrum
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del analizador de espectro: se comprueba que la FFT propia coloca un
 * tono de 1 kHz en la banda correcta (la cuarta de seis), que los niveles se
 * normalizan entre 0 y 1 y que 16 bits, float y estéreo dan el mismo resultado.
 */
class AnalizadorEspectroTest {

    private val frecuencia = 44_100

    /** Banda esperada para 1 kHz: la cuarta (graves → agudos). */
    private val bandaDelTono = 3

    /** Ventana completa del analizador. */
    private val muestras = 1024

    @Test
    fun `tono de 1 kHz en PCM 16 bits cae en la cuarta banda`() {
        val analizador = AnalizadorEspectro()
        analizador.configurar(frecuencia, 1, esFloat = false)

        val publicado = analizador.analizar(pcm16(1000.0, 0.5, canales = 1))

        assertTrue("la primera ventana debe publicar una medida", publicado)
        assertBandaDelTono(analizador.niveles)
    }

    @Test
    fun `tono de 1 kHz en PCM float estereo da el mismo resultado`() {
        val analizador = AnalizadorEspectro()
        analizador.configurar(frecuencia, 2, esFloat = true)

        val publicado = analizador.analizar(pcmFloat(1000.0, 0.5, canales = 2))

        assertTrue(publicado)
        assertBandaDelTono(analizador.niveles)
    }

    @Test
    fun `el silencio deja todos los niveles a cero`() {
        val analizador = AnalizadorEspectro()
        analizador.configurar(frecuencia, 1, esFloat = false)

        analizador.analizar(pcm16(1000.0, 0.0, canales = 1))

        assertTrue(analizador.niveles.all { it == 0f })
    }

    @Test
    fun `un formato sin muestras no publica nada`() {
        val analizador = AnalizadorEspectro()
        analizador.configurar(frecuencia, 1, esFloat = false)

        val vacio = ByteBuffer.allocate(0)
        assertTrue(!analizador.analizar(vacio))
    }

    /** Comprueba el tono: banda 3 destacada, el resto bajo, todos en 0..1. */
    private fun assertBandaDelTono(niveles: FloatArray) {
        assertEquals(AudioSpectrum.BANDAS, niveles.size)
        assertTrue(niveles.all { it in 0f..1f })

        val bandaMaxima = niveles.indices.maxBy { niveles[it] }
        assertEquals("la energía debe estar en la banda del tono", bandaDelTono, bandaMaxima)
        assertTrue("el tono debe dar un nivel alto: ${niveles[bandaDelTono]}", niveles[bandaDelTono] > 0.5f)
        niveles.indices
            .filter { it != bandaDelTono }
            .forEach { assertTrue("banda $it con fugas: ${niveles[it]}", niveles[it] < 0.35f) }
    }

    /** PCM 16 bits little-endian, mono o estéreo intercalado. */
    private fun pcm16(hercios: Double, amplitud: Double, canales: Int): ByteBuffer {
        val buffer = ByteBuffer.allocate(muestras * canales * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until muestras) {
            val valor = (amplitud * Short.MAX_VALUE * sin(2.0 * PI * hercios * i / frecuencia))
                .toInt().toShort()
            repeat(canales) { buffer.putShort(valor) }
        }
        buffer.flip()
        return buffer
    }

    /** PCM float little-endian, mono o estéreo intercalado. */
    private fun pcmFloat(hercios: Double, amplitud: Double, canales: Int): ByteBuffer {
        val buffer = ByteBuffer.allocate(muestras * canales * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until muestras) {
            val valor = (amplitud * sin(2.0 * PI * hercios * i / frecuencia)).toFloat()
            repeat(canales) { buffer.putFloat(valor) }
        }
        buffer.flip()
        return buffer
    }
}
