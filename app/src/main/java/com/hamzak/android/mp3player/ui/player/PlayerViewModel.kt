package com.hamzak.android.mp3player.ui.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.concurrent.futures.await
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.repository.SongRepository
import com.hamzak.android.mp3player.service.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private val mediaControllerFuture: ListenableFuture<MediaController> = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateReadyState { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                updateReadyState { it.copy(duration = mediaController?.duration ?: 0) }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null) return
            val currentSong = songs.find { it.id.toString() == mediaItem.mediaId }
            if (currentSong != null) {
                updateReadyState { it.copy(song = currentSong) }
            }
        }
    }

    private val songs: MutableList<Song> = mutableListOf()

    init {
        viewModelScope.launch {
            initializePlayer()
        }
    }

    private suspend fun initializePlayer() {
        try {
            mediaController = mediaControllerFuture.await()
            mediaController?.addListener(playerListener)

            val songId = savedStateHandle.get<Int>("songId")
            if (songId == null) {
                _uiState.update { PlayerUiState.Error("Song not found") }
                return
            }

            val allSongs = songRepository.getAllSongs().first()
            songs.clear()
            songs.addAll(allSongs)

            val initialSongIndex = songs.indexOfFirst { it.id == songId }
            if (initialSongIndex == -1) {
                _uiState.update { PlayerUiState.Error("Song not found") }
                return
            }

            val initialSong = songs[initialSongIndex]
            _uiState.update { PlayerUiState.Ready(song = initialSong) }

            val mediaItems = songs.map { song ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.path.toUri())
                    .build()
                MediaItem.Builder()
                    .setUri(song.path)
                    .setMediaId(song.id.toString())
                    .setMediaMetadata(metadata)
                    .build()
            }

            mediaController?.setMediaItems(mediaItems, initialSongIndex, 0)
            mediaController?.prepare()
            mediaController?.play()

            startPositionUpdates()

        } catch (e: Exception) {
            _uiState.update { PlayerUiState.Error("Failed to connect to media service") }
        }
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                updateReadyState { state ->
                    if (!state.isSeeking) {
                        state.copy(currentPosition = mediaController?.currentPosition ?: 0)
                    } else {
                        state
                    }
                }
                delay(200)
            }
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
        updateReadyState { it.copy(isSeeking = true, currentPosition = position) }
        mediaController?.seekTo(position)
        // We need to reset isSeeking after the seek is complete.
        viewModelScope.launch {
            delay(200) // Give time for the seek to process
            updateReadyState { it.copy(isSeeking = false) }
        }
    }

    private fun updateReadyState(update: (PlayerUiState.Ready) -> PlayerUiState) {
        _uiState.update { currentState ->
            if (currentState is PlayerUiState.Ready) update(currentState) else currentState
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(playerListener)
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
