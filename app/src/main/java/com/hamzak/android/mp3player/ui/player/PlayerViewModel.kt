package com.hamzak.android.mp3player.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.local.SongDao
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
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
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Ready(val song: Song) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}
