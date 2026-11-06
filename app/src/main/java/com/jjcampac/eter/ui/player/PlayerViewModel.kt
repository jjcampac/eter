package com.jjcampac.eter.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.RadioPlayer
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel del reproductor.
 *
 * Delega todas las operaciones en el [RadioPlayer] compartido, de modo que
 * el estado sea coherente entre la pantalla del reproductor y el mini reproductor.
 */
class PlayerViewModel(
    private val radioPlayer: RadioPlayer
) : ViewModel() {

    val playerState: StateFlow<com.jjcampac.eter.player.PlayerState> = radioPlayer.state

    val isPlaying: Boolean
        get() = playerState.value.status == PlaybackStatus.PLAYING

    /** Reproduce la emisora o la pausa si ya es la actual y está sonando. */
    fun togglePlay(station: Station) {
        val current = playerState.value
        if (current.status == PlaybackStatus.PLAYING && current.currentStation?.id == station.id) {
            radioPlayer.pause()
        } else if (current.status == PlaybackStatus.PAUSED && current.currentStation?.id == station.id) {
            radioPlayer.resume()
        } else {
            radioPlayer.play(station)
        }
    }

    fun play(station: Station) = radioPlayer.play(station)

    fun pause() = radioPlayer.pause()

    fun resume() = radioPlayer.resume()

    fun stop() = radioPlayer.stop()

    companion object {
        /** Fábrica simple (sin Hilt) a partir del reproductor compartido. */
        fun factory(radioPlayer: RadioPlayer) = viewModelFactory {
            initializer { PlayerViewModel(radioPlayer) }
        }
    }
}
