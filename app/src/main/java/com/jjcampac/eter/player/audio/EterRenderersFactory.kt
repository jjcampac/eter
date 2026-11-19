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

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Fábrica de renderers que inserta el [EspectroAudioProcessor] en la cadena de
 * procesadores del [DefaultAudioSink] de Media3.
 *
 * Es el punto que usa ExoPlayer para crear el sumidero de audio, así que vale
 * tanto para streams MP3/AAC (Icecast/Shoutcast) como para HLS. El procesador va
 * antes del ajuste de velocidad y de la omisión de silencio, por lo que recibe
 * el audio ya decodificado, sin retoques, y lo reenvía sin modificarlo.
 */
@UnstableApi
class EterRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    /** Analizador compartido con la UI a través de [AudioSpectrum]. */
    private val procesadorEspectro = EspectroAudioProcessor()

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        val cadena = DefaultAudioSink.DefaultAudioProcessorChain(procesadorEspectro)
        return DefaultAudioSink.Builder(context)
            .setAudioProcessorChain(cadena)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .build()
    }
}
