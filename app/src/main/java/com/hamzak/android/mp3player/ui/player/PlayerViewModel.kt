package com.hamzak.android.mp3player.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.local.SongDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val songDao: SongDao,
    private val exoPlayer: ExoPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val currentState = _uiState.value
            if (currentState is PlayerUiState.Ready) {
                _uiState.value = currentState.copy(isPlaying = isPlaying)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val currentState = _uiState.value
                if (currentState is PlayerUiState.Ready) {
                    _uiState.value = currentState.copy(duration = exoPlayer.duration)
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                val currentState = _uiState.value
                if (currentState is PlayerUiState.Ready && currentState.isSeeking) {
                    _uiState.value = currentState.copy(
                        isSeeking = false,
                        currentPosition = newPosition.positionMs
                    )
                }
            }
        }
    }

    init {
        exoPlayer.addListener(listener)

        viewModelScope.launch {
            val songId = savedStateHandle.get<Int>("songId")
            if (songId != null) {
                val song = songDao.getSongById(songId)
                if (song != null) {
                    _uiState.value = PlayerUiState.Ready(song)
                    exoPlayer.setMediaItem(MediaItem.fromUri(song.path))
                    exoPlayer.prepare()
                } else {
                    _uiState.value = PlayerUiState.Error("Song not found")
                }
            } else {
                _uiState.value = PlayerUiState.Error("Song ID not provided")
            }
        }

        viewModelScope.launch {
            while (true) {
                val currentState = _uiState.value
                if (currentState is PlayerUiState.Ready && !currentState.isSeeking) {
                    _uiState.value = currentState.copy(currentPosition = exoPlayer.currentPosition)
                }
                delay(200)
            }
        }
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(position: Long) {
        val currentState = _uiState.value
        if (currentState is PlayerUiState.Ready) {
            _uiState.value = currentState.copy(
                isSeeking = true,
                currentPosition = position
            )
        }
        exoPlayer.seekTo(position)
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }
}

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Ready(
        val song: Song,
        val isPlaying: Boolean = false,
        val duration: Long = 0,
        val currentPosition: Long = 0,
        val isSeeking: Boolean = false
    ) : PlayerUiState()

    data class Error(val message: String) : PlayerUiState()
}
