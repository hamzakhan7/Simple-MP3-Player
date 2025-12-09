package com.hamzak.android.mp3player.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.concurrent.futures.await
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.local.SongDao
import com.hamzak.android.mp3player.service.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val songDao: SongDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val mediaControllerFuture: ListenableFuture<MediaController> = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()
    private var mediaController: MediaController? = null

    private var songs: List<Song> = emptyList()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update {
                if (it is PlayerUiState.Ready) it.copy(isPlaying = isPlaying) else it
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            mediaController?.let { controller ->
                if (playbackState == Player.STATE_READY) {
                    _uiState.update {
                        if (it is PlayerUiState.Ready) it.copy(duration = controller.duration) else it
                    }
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                _uiState.update {
                    if (it is PlayerUiState.Ready && it.isSeeking) {
                        it.copy(isSeeking = false, currentPosition = newPosition.positionMs)
                    } else {
                        it
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            mediaController?.let { controller ->
                if (controller.mediaItemCount == 0) return@let
                val song = songs[controller.currentMediaItemIndex]
                _uiState.update {
                    if (it is PlayerUiState.Ready) {
                        it.copy(song = song)
                    } else {
                        PlayerUiState.Ready(song = song)
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            if (!connectToController()) return@launch
            if (!loadInitialSongs()) return@launch

            startPositionUpdates()
        }
    }

    private suspend fun connectToController(): Boolean {
        return try {
            mediaController = mediaControllerFuture.await()
            mediaController?.addListener(listener)
            true
        } catch (e: Exception) {
            _uiState.update { PlayerUiState.Error("Failed to connect to media service") }
            false
        }
    }

    private suspend fun loadInitialSongs(): Boolean {
        val songId = savedStateHandle.get<Int>("songId")
        if (songId == null) {
            _uiState.update { PlayerUiState.Error("Song ID not provided") }
            return false
        }

        songs = songDao.getAllSongs().first()
        val initialSongIndex = songs.indexOfFirst { it.id == songId }

        if (initialSongIndex != -1) {
            val initialSong = songs[initialSongIndex]
            _uiState.update { PlayerUiState.Ready(song = initialSong) }
            val mediaItems = songs.map { MediaItem.fromUri(it.path) }
            mediaController?.setMediaItems(mediaItems, initialSongIndex, 0)
            mediaController?.prepare()
            return true
        } else {
            _uiState.update { PlayerUiState.Error("Song not found") }
            return false
        }
    }

    private suspend fun startPositionUpdates() {
        while (true) {
            mediaController?.let { controller ->
                _uiState.update {
                    if (it is PlayerUiState.Ready && !it.isSeeking) {
                        it.copy(currentPosition = controller.currentPosition)
                    } else {
                        it
                    }
                }
            }
            delay(200)
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun seekTo(position: Long) {
        _uiState.update {
            if (it is PlayerUiState.Ready) {
                it.copy(isSeeking = true, currentPosition = position)
            } else {
                it
            }
        }
        mediaController?.seekTo(position)
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(listener)
        MediaController.releaseFuture(mediaControllerFuture)
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
