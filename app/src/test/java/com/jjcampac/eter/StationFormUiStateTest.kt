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

import com.jjcampac.eter.ui.station.StationFormUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la validación del formulario de emisora.
 *
 * Cubre lo obligatorio (nombre y URL del stream) y la regla del favicon opcional:
 * vale vacío o una URL que empiece por http/https.
 */
class StationFormUiStateTest {

    @Test
    fun `nombre y url del stream son obligatorios`() {
        assertFalse(StationFormUiState().isValid)
        assertFalse(StationFormUiState(name = "Radio", url = "").isValid)
        assertFalse(StationFormUiState(name = "", url = "http://ejemplo.com/s.mp3").isValid)
        assertFalse(StationFormUiState(name = "Radio", url = "mms://ejemplo.com/s").isValid)
        assertTrue(StationFormUiState(name = "Radio", url = "http://ejemplo.com/s.mp3").isValid)
        assertTrue(StationFormUiState(name = "Radio", url = "https://ejemplo.com/s.mp3").isValid)
    }

    @Test
    fun `el favicon vacio es valido porque es opcional`() {
        assertTrue(StationFormUiState(imageUrl = "").hasValidImageUrl)
        assertTrue(StationFormUiState(imageUrl = "   ").hasValidImageUrl)
    }

    @Test
    fun `el favicon debe empezar por http o https`() {
        assertTrue(StationFormUiState(imageUrl = "http://ejemplo.com/logo.png").hasValidImageUrl)
        assertTrue(
            StationFormUiState(imageUrl = "  https://ejemplo.com/logo.png").hasValidImageUrl
        )
        assertFalse(StationFormUiState(imageUrl = "ejemplo.com/logo.png").hasValidImageUrl)
        assertFalse(StationFormUiState(imageUrl = "ftp://ejemplo.com/logo.png").hasValidImageUrl)
    }

    @Test
    fun `los espacios dentro de las url se ignoran`() {
        // Artefacto típico al pegar o teclear: "http://ejemplo. com/s. mp3".
        val state = StationFormUiState(
            name = "Radio",
            url = "http://ejemplo. com/s. mp3",
            imageUrl = "https://picsum. photos/200"
        )
        assertEquals("http://ejemplo.com/s.mp3", state.urlSinEspacios)
        assertEquals("https://picsum.photos/200", state.imageUrlSinEspacios)
        assertTrue(state.isValid)
        assertTrue(state.hasValidImageUrl)
    }

    @Test
    fun `una url de stream con solo espacios no es valida`() {
        assertFalse(StationFormUiState(name = "Radio", url = "   ").isValid)
    }

    @Test
    fun `los espacios no invalidan una url correcta`() {
        assertTrue(StationFormUiState(name = "Radio", url = "http: //e. com").isValid)
    }
}
