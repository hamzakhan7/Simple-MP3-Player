package com.hamzak.android.mp3player.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    PlayerScreen(
        uiState = uiState,
        onPlayPauseClick = {
            if (viewModel.uiState.value is PlayerUiState.Ready) {
                if ((viewModel.uiState.value as PlayerUiState.Ready).isPlaying) {
                    viewModel.pause()
                } else {
                    viewModel.play()
                }
            }
        },
        onSeek = viewModel::seekTo
    )
}

@Composable
private fun PlayerScreen(
    uiState: PlayerUiState,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (uiState) {
            is PlayerUiState.Loading -> {
                CircularProgressIndicator()
            }
            is PlayerUiState.Error -> {
                Text(text = uiState.message)
            }
            is PlayerUiState.Ready -> {
                var sliderPosition by remember { mutableFloatStateOf(uiState.currentPosition.toFloat()) }
                var isScrubbing by remember { mutableStateOf(false) }

                LaunchedEffect(uiState.currentPosition, isScrubbing) {
                    if (!isScrubbing) {
                        sliderPosition = uiState.currentPosition.toFloat()
                    }
                }

                Text(text = uiState.song.title)
                Text(text = uiState.song.artist)

                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        isScrubbing = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        onSeek(sliderPosition.toLong())
                        isScrubbing = false
                    },
                    valueRange = 0f..uiState.duration.toFloat(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatDuration(sliderPosition.toLong()))
                    Text(text = formatDuration(uiState.duration))
                }

                Button(onClick = onPlayPauseClick) {
                    Text(if (uiState.isPlaying) "Pause" else "Play")
                }
            }
        }
    }
}

private fun formatDuration(duration: Long): String {
    val minutes = (duration / 1000 / 60).toString().padStart(2, '0')
    val seconds = (duration / 1000 % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
