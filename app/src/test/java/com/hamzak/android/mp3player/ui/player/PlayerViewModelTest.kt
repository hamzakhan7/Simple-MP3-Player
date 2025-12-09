package com.hamzak.android.mp3player.ui.player

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.concurrent.futures.await
import androidx.lifecycle.SavedStateHandle
import androidx.media3.session.MediaController
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.repository.SongRepository
import com.hamzak.android.mp3player.util.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PlayerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var songRepository: SongRepository
    private lateinit var context: Context
    private lateinit var viewModel: PlayerViewModel

    private lateinit var mockMediaController: MediaController

    private val testSong = Song(1, "Title", "Artist", "Album", 120, "/path/to/song")
    private val testSongs = listOf(testSong)

    @Before
    fun setup() {
        savedStateHandle = mockk(relaxed = true)
        every { savedStateHandle.get<Int>("songId") } returns 1

        songRepository = mockk()
        every { songRepository.getAllSongs() } returns flowOf(testSongs)

        context = mockk(relaxed = true)

        // Mock the constructor of MediaController.Builder to avoid calling Android framework code
        mockkConstructor(MediaController.Builder::class)
        val mockFuture = mockk<com.google.common.util.concurrent.ListenableFuture<MediaController>>()
        mockMediaController = mockk(relaxed = true)

        // When buildAsync() is called on any MediaController.Builder, return our mock future
        every { anyConstructed<MediaController.Builder>().buildAsync() } returns mockFuture
        coEvery { mockFuture.await() } returns mockMediaController

        // This will trigger the init block, which uses the mocked builder
        viewModel = PlayerViewModel(savedStateHandle, songRepository, context)
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `init loads songs, sets media items, prepares and plays`() = runTest {
        coVerify { songRepository.getAllSongs() }
        coVerify { mockMediaController.setMediaItems(any(), 0, 0) }
        coVerify { mockMediaController.prepare() }
        coVerify { mockMediaController.play() }
    }

    @Test
    fun `play calls mediaController's play`() = runTest {
        viewModel.play()
        coVerify { mockMediaController.play() }
    }

    @Test
    fun `pause calls mediaController's pause`() = runTest {
        viewModel.pause()
        coVerify { mockMediaController.pause() }
    }

    @Test
    fun `skipToNext calls mediaController's seekToNext`() = runTest {
        viewModel.skipToNext()
        coVerify { mockMediaController.seekToNext() }
    }

    @Test
    fun `skipToPrevious calls mediaController's seekToPrevious`() = runTest {
        viewModel.skipToPrevious()
        coVerify { mockMediaController.seekToPrevious() }
    }

    @Test
    fun `seekTo calls mediaController's seekTo`() = runTest {
        val seekPosition = 12345L
        viewModel.seekTo(seekPosition)
        coVerify { mockMediaController.seekTo(seekPosition) }
    }
}
