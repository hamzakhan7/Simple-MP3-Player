package com.hamzak.android.mp3player.ui.player

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hamzak.android.mp3player.data.local.Song

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is PlayerUiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is PlayerUiState.Ready -> {
            PlayerContent(
                state = state,
                onPlay = { viewModel.play() },
                onPause = { viewModel.pause() },
                onSkipToNext = { viewModel.skipToNext() },
                onSkipToPrevious = { viewModel.skipToPrevious() },
                onSeek = { viewModel.seekTo(it) }
            )
        }

        is PlayerUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = state.message)
            }
        }
    }
}

@Composable
fun PlayerContent(
    state: PlayerUiState.Ready,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSkipToNext: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AlbumArt(songPath = state.song.path, modifier = Modifier.size(200.dp))
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SongDetails(song = state.song)
                Spacer(modifier = Modifier.height(16.dp))
                PlayerSlider(state = state, onSeek = onSeek)
                PlayerControls(
                    isPlaying = state.isPlaying,
                    onPlay = onPlay,
                    onPause = onPause,
                    onSkipToPrevious = onSkipToPrevious,
                    onSkipToNext = onSkipToNext
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AlbumArt(songPath = state.song.path, modifier = Modifier.size(300.dp))
            Spacer(modifier = Modifier.height(32.dp))
            SongDetails(song = state.song)
            Spacer(modifier = Modifier.height(32.dp))
            PlayerSlider(state = state, onSeek = onSeek)
            PlayerControls(
                isPlaying = state.isPlaying,
                onPlay = onPlay,
                onPause = onPause,
                onSkipToPrevious = onSkipToPrevious,
                onSkipToNext = onSkipToNext
            )
        }
    }
}

@Composable
private fun AlbumArt(songPath: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Music Note Placeholder",
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Composable
private fun SongDetails(song: Song, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = song.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = song.artist, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlayerSlider(state: PlayerUiState.Ready, onSeek: (Long) -> Unit) {
    Slider(
        value = state.currentPosition.toFloat(),
        onValueChange = { onSeek(it.toLong()) },
        valueRange = 0f..state.duration.toFloat(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSkipToPrevious) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Skip Previous", modifier = Modifier.size(48.dp))
        }
        IconButton(onClick = { if (isPlaying) onPause() else onPlay() }) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(72.dp)
            )
        }
        IconButton(onClick = onSkipToNext) {
            Icon(Icons.Default.SkipNext, contentDescription = "Skip Next", modifier = Modifier.size(48.dp))
        }
    }
}
