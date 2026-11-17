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
package com.jjcampac.eter.ui

import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.PlayerState
import com.jjcampac.eter.player.SleepTimer
import com.jjcampac.eter.ui.main.MainUiState
import com.jjcampac.eter.ui.main.SearchSource
import com.jjcampac.eter.ui.station.StationFormUiState

/**
 * Datos estáticos y realistas para las `@Preview` de la app.
 *
 * Todo es local: no hay red, ni ViewModels, ni imágenes remotas. Las emisoras
 * no llevan `imageUrl` a propósito, de modo que los iconos muestran sus
 * iniciales y las previews se renderizan siempre igual, sin descargas.
 */
object PreviewData {

    // --- Emisoras ------------------------------------------------------------

    val copeMadrid = Station(
        id = "preview-cope-madrid",
        name = "Cope Madrid FM",
        url = "https://stream.example.com/cope-madrid",
        country = "Spain",
        isFavorite = true,
        sortOrder = 0
    )

    val copeSevilla = Station(
        id = "preview-cope-sevilla",
        name = "Cope Sevilla",
        url = "https://stream.example.com/cope-sevilla",
        country = "Spain",
        genre = "noticias",
        isFavorite = true,
        sortOrder = 1
    )

    val esRadioMadrid = Station(
        id = "preview-esradio-madrid",
        name = "esRadio Madrid",
        url = "https://stream.example.com/esradio-madrid",
        country = "Spain",
        isFavorite = true,
        sortOrder = 2
    )

    val radioLibertad = Station(
        id = "preview-radio-libertad",
        name = "Radio Libertad",
        url = "https://stream.example.com/radio-libertad",
        country = "Spain",
        isFavorite = true,
        sortOrder = 3
    )

    val ondaCeroSevilla = Station(
        id = "preview-onda-cero-sevilla",
        name = "Onda Cero Sevilla",
        url = "https://stream.example.com/onda-cero-sevilla",
        country = "Spain",
        genre = "noticias",
        isFavorite = true,
        sortOrder = 4
    )

    val cadenaCien = Station(
        id = "preview-cadena-100",
        name = "Cadena 100",
        url = "https://stream.example.com/cadena-100",
        country = "Spain",
        genre = "music",
        isFavorite = true,
        sortOrder = 5
    )

    /** Emisora añadida a mano (la única que aparece en "Mis emisoras"). */
    val emisoraManual = Station(
        id = "preview-manual-somafm",
        name = "SomaFM Groove Salad",
        url = "https://ice1.somafm.com/groovesalad-128-mp3",
        country = "EE. UU.",
        genre = "Chillout / Downtempo",
        isCustom = true
    )

    /** Favoritas en el orden en que se muestran en la pestaña "Favoritas". */
    val favoritas: List<Station> = listOf(
        copeMadrid,
        copeSevilla,
        esRadioMadrid,
        radioLibertad,
        ondaCeroSevilla,
        cadenaCien
    )

    /** Catálogo local de ejemplo ("Estaciones" → "Mis emisoras"). */
    val emisorasManuales: List<Station> = listOf(
        emisoraManual,
        Station(
            id = "preview-manual-flamenco",
            name = "Flamenco Radio Sevilla",
            url = "https://stream.example.com/flamenco-radio",
            country = "Spain",
            genre = "flamenco",
            isCustom = true
        )
    )

    /** Resultados de ejemplo de Radio Browser ("Estaciones" → "Internet"). */
    val resultadosInternet: List<Station> = listOf(
        copeSevilla.copy(isFavorite = true),
        radioLibertad,
        ondaCeroSevilla,
        cadenaCien
    )

    // --- Reproductor ---------------------------------------------------------

    fun reproduciendo(station: Station = copeSevilla): PlayerState = PlayerState(
        status = PlaybackStatus.PLAYING,
        currentStation = station,
        metadataTitle = "Coldplay — Viva la Vida"
    )

    fun enPausa(station: Station = copeSevilla): PlayerState = PlayerState(
        status = PlaybackStatus.PAUSED,
        currentStation = station
    )

    fun cargando(station: Station = copeSevilla): PlayerState = PlayerState(
        status = PlaybackStatus.LOADING,
        currentStation = station
    )

    fun conError(
        station: Station = copeSevilla,
        mensaje: String = "No se pudo conectar con la emisora"
    ): PlayerState = PlayerState(
        status = PlaybackStatus.ERROR,
        currentStation = station,
        errorMessage = mensaje
    )

    // --- Temporizador para dormir -------------------------------------------

    val temporizadorApagado = SleepTimer.State()

    val temporizadorActivo = SleepTimer.State(
        active = true,
        remainingMillis = 20 * 60_000L + 5_000L
    )

    // --- Estado de la pantalla "Estaciones" ----------------------------------

    /** "Internet" con resultados ya cargados. */
    val mainInternetConResultados = MainUiState(
        query = "cope",
        source = SearchSource.INTERNET,
        country = "España",
        genre = "",
        remoteStations = resultadosInternet,
        favoriteIds = setOf(copeSevilla.id)
    )

    /** "Internet" con la búsqueda en curso. */
    val mainInternetBuscando = MainUiState(
        query = "onda cero",
        source = SearchSource.INTERNET,
        country = "España",
        isSearching = true
    )

    /** "Internet" con error de conexión con Radio Browser. */
    val mainInternetConError = MainUiState(
        query = "cadena 100",
        source = SearchSource.INTERNET,
        country = "España",
        hasSearchError = true
    )

    /** "Internet" sin consulta escrita (mensaje de ayuda). */
    val mainInternetSinConsulta = MainUiState(
        query = "",
        source = SearchSource.INTERNET,
        country = "España"
    )

    /** "Mis emisoras" con emisoras manuales. */
    val mainLocalConEmisoras = MainUiState(
        query = "",
        source = SearchSource.LOCAL,
        country = "España",
        stations = emisorasManuales,
        favoriteIds = setOf(emisoraManual.id)
    )

    /** "Mis emisoras" vacía. */
    val mainLocalVacia = MainUiState(
        query = "",
        source = SearchSource.LOCAL,
        country = "España"
    )

    // --- Formulario de emisora ----------------------------------------------

    /** Alta: formulario en blanco. */
    val formularioNuevo = StationFormUiState()

    /** Edición: emisora cargada con sus datos. */
    val formularioEdicion = StationFormUiState(
        id = emisoraManual.id,
        name = emisoraManual.name,
        url = emisoraManual.url,
        country = emisoraManual.country.orEmpty(),
        genre = emisoraManual.genre.orEmpty(),
        isEditing = true
    )

    /** Guardando: botón deshabilitado con el indicador de progreso. */
    val formularioGuardando = formularioEdicion.copy(isSaving = true)

    /** Error al guardar. */
    val formularioConError = formularioNuevo.copy(
        name = "Radio de prueba",
        url = "https://stream.example.com/radio-prueba",
        error = "No se pudo guardar la emisora"
    )
}
