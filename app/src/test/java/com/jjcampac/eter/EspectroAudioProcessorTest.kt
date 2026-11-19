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

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.jjcampac.eter.player.audio.EspectroAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del procesador de audio: es un paso directo (no altera el sonido) y
 * debe aguantar los buffers vacíos con los que Media3 alimenta el pipeline.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class EspectroAudioProcessorTest {

    private val formato = AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT)

    @Test
    fun `el audio sale igual que entra`() {
        val procesador = EspectroAudioProcessor()
        procesador.configure(formato)
        procesador.flush()

        val entrada = pcm16(muestras = 1024)
        val copia = entrada.duplicate().order(ByteOrder.LITTLE_ENDIAN)

        procesador.queueInput(entrada)
        val salida = procesador.output

        assertEquals("el tamaño no cambia", copia.remaining(), salida.remaining())
        while (copia.hasRemaining()) {
            assertEquals("el audio se copia sin modificar", copia.getShort(), salida.getShort())
        }
        procesador.reset()
    }

    @Test
    fun `un buffer vacio no lanza excepcion`() {
        val procesador = EspectroAudioProcessor()
        procesador.configure(formato)
        procesador.flush()

        // Media3 llama a queueInput con AudioProcessor.EMPTY_BUFFER: con
        // replaceOutputBuffer(0) el buffer interno es el mismo objeto vacío y
        // copiarlo sobre sí mismo lanzaba IllegalArgumentException.
        procesador.queueInput(AudioProcessor.EMPTY_BUFFER)

        assertTrue("sin datos no hay salida", !procesador.output.hasRemaining())
        procesador.reset()
    }

    @Test
    fun `el formato de salida es el mismo que el de entrada`() {
        val procesador = EspectroAudioProcessor()

        val configurado = procesador.configure(formato)

        assertSame(formato, configurado)
        assertTrue(procesador.isActive)
        procesador.reset()
    }

    @Test
    fun `un formato no soportado deja el procesador inactivo`() {
        val procesador = EspectroAudioProcessor()

        // PCM de 8 bits: no se analiza y el audio debe pasar sin tocar.
        val noSoportado = AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_8BIT)
        procesador.configure(noSoportado)

        assertTrue(!procesador.isActive)
        procesador.reset()
    }

    /** PCM 16 bits estéreo little-endian con un tono suave. */
    private fun pcm16(muestras: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(muestras * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until muestras) {
            val valor = (8000 * kotlin.math.sin(i * 0.05)).toInt().toShort()
            buffer.putShort(valor)
            buffer.putShort(valor)
        }
        buffer.flip()
        return buffer
    }
}
