package com.hamzak.android.mp3player.data.repository

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.local.SongDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val application: Application
) {

    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    suspend fun addSong(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val contentResolver = application.contentResolver
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val metadataRetriever = MediaMetadataRetriever()
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

                metadataRetriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteSong(song: Song) {
        withContext(Dispatchers.IO) {
            songDao.deleteSong(song.id)
        }
    }
}
