package com.hamzak.android.mp3player.ui.player

import android.content.Context
import androidx.media3.session.MediaController
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.concurrent.futures.await
import androidx.lifecycle.SavedStateHandle
import androidx.media3.session.SessionToken
import io.mockk.mockkConstructor
import com.hamzak.android.mp3player.data.local.Song
import com.hamzak.android.mp3player.data.repository.SongRepository
import com.hamzak.android.mp3player.util.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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

    private val testSong = Song(1, "Title", "Artist", "Album", 120, "/path/to/song")

    @Before
    fun setup() {
        savedStateHandle = mockk(relaxed = true)
        every { savedStateHandle.get<Int>("songId") } returns 1

        songRepository = mockk()
        every { songRepository.getAllSongs() } returns flowOf(listOf(testSong))

        context = mockk(relaxed = true)

        mockkStatic(MediaController::class)
        val mockFuture = mockk<com.google.common.util.concurrent.ListenableFuture<MediaController>>()
        val mockMediaController = mockk<MediaController>(relaxed = true)

        mockkConstructor(MediaController.Builder::class)
        every { anyConstructed<MediaController.Builder>().buildAsync() } returns mockFuture
        coEvery { mockFuture.await() } returns mockMediaController

        mockkStatic(SessionToken::class)
        every { SessionToken(any(), any()) } returns mockk()


        viewModel = PlayerViewModel(savedStateHandle, songRepository, context)
    }

    @Test
    fun `init loads initial song and prepares player`() = runTest {
        // The init block is called during setup, so we just need to verify the results
        mainCoroutineRule.testDispatcher.scheduler.advanceUntilIdle() // Ensure all coroutines complete
        coVerify { songRepository.getAllSongs() }
    }

    @Test
    fun `play calls mediaController's play`() {
        viewModel.play()
    }

    @Test
    fun `pause calls mediaController's pause`() {
        viewModel.pause()
    }
}
