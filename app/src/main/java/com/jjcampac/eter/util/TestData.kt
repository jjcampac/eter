package com.jjcampac.eter.util

import com.jjcampac.eter.domain.model.Station

/**
 * Datos estáticos de prueba (Fase 1).
 *
 * En las Fases 3 y 4 estas emisoras se sustituyen por las que devuelvan
 * el repositorio (Room + API de Radio Browser).
 */
object TestData {

    /** Emisora fija usada para validar la reproducción en esta fase. */
    val testStation = Station(
        id = "test-somafm",
        name = "SomaFM Groove Salad",
        url = "https://ice1.somafm.com/groovesalad-128-mp3",
        country = "EE. UU.",
        genre = "Chillout / Downtempo"
    )

    val all: List<Station> = listOf(testStation)
}
