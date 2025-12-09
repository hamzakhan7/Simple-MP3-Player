package com.hamzak.android.mp3player.data.repository

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.text.input.KeyboardType
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.local.SongDao
import com.hamzak.android.mp3player.util.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SongRepositoryTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var songDao: SongDao
    private lateinit var application: Application
    private lateinit var repository: SongRepository

    private val testSong = Song(1, "Title", "Artist", "Album", 120, "/path/to/song")

    @Before
    fun setup() {
        songDao = mockk(relaxed = true)
        application = mockk(relaxed = true)
        repository = SongRepository(songDao, application)
    }

    @Test
    fun `getAllSongs returns flow of songs from DAO`() = runTest {
        every { songDao.getAllSongs() } returns flowOf(listOf(testSong))
        val songs = repository.getAllSongs().first()
        assertEquals(1, songs.size)
        assertEquals(testSong, songs[0])
    }

    @Test
    fun `addSong inserts song into DAO`() = runTest {
        val uri = mockk<android.net.Uri>()
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every { application.contentResolver } returns contentResolver

        mockkConstructor(MediaMetadataRetriever::class)
        every { anyConstructed<MediaMetadataRetriever>().setDataSource(any<Context>(), any<Uri>()) } returns Unit
        every { anyConstructed<MediaMetadataRetriever>().close() } returns Unit
        coEvery { anyConstructed<MediaMetadataRetriever>().extractMetadata(any()) } returns "Test"
        coEvery { anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) } returns "120000"

        repository.addSong(uri)

        coVerify { songDao.insertSong(any()) }
    }

    @Test
    fun `deleteSong deletes song from DAO`() = runTest {
        repository.deleteSong(testSong)
        coVerify { songDao.deleteSong(testSong.id) }
    }
}
