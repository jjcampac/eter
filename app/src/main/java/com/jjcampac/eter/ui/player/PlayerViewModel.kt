/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.domain.repository.StationRepository
import com.jjcampac.eter.player.RadioPlayer
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel del reproductor.
 *
 * Delega todas las operaciones en el [RadioPlayer] compartido, de modo que
 * el estado sea coherente entre la pantalla del reproductor y el mini reproductor.
 */
class PlayerViewModel(
    private val radioPlayer: RadioPlayer,
    private val repository: StationRepository? = null
) : ViewModel() {

    val playerState: StateFlow<com.jjcampac.eter.player.PlayerState> = radioPlayer.state

    val isPlaying: Boolean
        get() = playerState.value.status == PlaybackStatus.PLAYING

    /** Reproduce la emisora o la pausa si ya es la actual y está sonando. */
    fun togglePlay(station: Station) {
        val current = playerState.value
        if (current.status == PlaybackStatus.PLAYING && current.currentStation?.url == station.url) {
            radioPlayer.pause()
        } else if (current.status == PlaybackStatus.PAUSED && current.currentStation?.url == station.url) {
            radioPlayer.resume()
        } else {
            radioPlayer.play(station)
        }
    }

    fun play(station: Station) = radioPlayer.play(station)

    fun pause() = radioPlayer.pause()

    fun resume() = radioPlayer.resume()

    fun stop() = radioPlayer.stop()

    /** Reproduce la siguiente favorita (circular). */
    fun nextFavorite() {
        val repo = repository ?: return
        viewModelScope.launch {
            val favs = repo.getFavoritesList()
            if (favs.isEmpty()) return@launch
            val currentUrl = playerState.value.currentStation?.url
            val idx = favs.indexOfFirst { it.url == currentUrl }
            val next = if (idx < 0 || idx == favs.lastIndex) favs.first() else favs[idx + 1]
            radioPlayer.play(next)
        }
    }

    /** Reproduce la favorita anterior (circular). */
    fun prevFavorite() {
        val repo = repository ?: return
        viewModelScope.launch {
            val favs = repo.getFavoritesList()
            if (favs.isEmpty()) return@launch
            val currentUrl = playerState.value.currentStation?.url
            val idx = favs.indexOfFirst { it.url == currentUrl }
            val prev = if (idx <= 0) favs.last() else favs[idx - 1]
            radioPlayer.play(prev)
        }
    }

    companion object {
        /** Fábrica simple (sin Hilt) a partir del reproductor compartido. */
        fun factory(radioPlayer: RadioPlayer, repository: StationRepository? = null) = viewModelFactory {
            initializer { PlayerViewModel(radioPlayer, repository) }
        }
    }
}
