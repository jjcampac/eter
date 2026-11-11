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
package com.jjcampac.eter.util

import com.jjcampac.eter.domain.model.Station

/**
 * Datos estáticos de prueba (Fase 1).
 *
 * En las Fases 3 y 4 estas emisoras se sustituyen por las que devuelvan
 * el repositorio (Room + API de Radio Browser).
 */
object TestData {

    /**
     * Emisora fija usada para validar la reproducción sin depender de la API
     * (decisión del proyecto en Fase 1). Se guarda con `isCustom = true` para
     * que aparezca como una emisora añadida a mano en "Mis Emisoras".
     */
    val testStation = Station(
        id = "test-somafm",
        name = "SomaFM Groove Salad",
        url = "https://ice1.somafm.com/groovesalad-128-mp3",
        country = "EE. UU.",
        genre = "Chillout / Downtempo",
        isCustom = true
    )

    val all: List<Station> = listOf(testStation)
}
