package com.hamzak.android.mp3player.ui.library

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.local.SongDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songDao: SongDao,
    private val application: Application
) : ViewModel() {

    val songs: StateFlow<List<Song>> = songDao.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addSong(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadataRetriever = MediaMetadataRetriever()
            try {
                metadataRetriever.setDataSource(application, uri)

                val title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
                val artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                val album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                val duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L

                val song = Song(
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    path = uri.toString()
                )

                songDao.insertSong(song)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                metadataRetriever.release()
            }
        }
    }
}
