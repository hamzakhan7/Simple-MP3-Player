package com.hamzak.android.mp3player.ui.library

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.repository.SongRepository
import com.hamzak.android.mp3player.util.MainCoroutineRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LibraryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var songRepository: SongRepository
    private lateinit var viewModel: LibraryViewModel

    private val testSong = Song(1, "Title", "Artist", "Album", 120, "/path/to/song")

    @Before
    fun setup() {
        songRepository = mockk(relaxed = true) // relaxed = true allows us to skip mocking every single function
        every { songRepository.getAllSongs() } returns flowOf(listOf(testSong))
        viewModel = LibraryViewModel(songRepository)
    }

    @Test
    fun `songs StateFlow correctly emits songs from repository`() = runTest {
        val songs = viewModel.songs.first()
        assertEquals(1, songs.size)
        assertEquals(testSong, songs[0])
    }

    @Test
    fun `addSong calls repository's addSong`() = runTest {
        val uri = mockk<android.net.Uri>()
        viewModel.addSong(uri)
        coVerify { songRepository.addSong(uri) }
    }

    @Test
    fun `deleteSong calls repository's deleteSong`() = runTest {
        viewModel.deleteSong(testSong)
        coVerify { songRepository.deleteSong(testSong) }
    }
}
