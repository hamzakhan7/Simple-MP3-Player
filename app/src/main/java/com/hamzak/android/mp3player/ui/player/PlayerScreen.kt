package com.hamzak.android.mp3player.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    PlayerScreen(
        uiState = uiState,
        onPlayClick = viewModel::play,
        onPauseClick = viewModel::pause
    )
}

@Composable
private fun PlayerScreen(
    uiState: PlayerUiState,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit
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
            is PlayerUiState.Ready -> {
                Text(text = uiState.song.title)
                Text(text = uiState.song.artist)
                Row {
                    Button(onClick = onPlayClick) {
                        Text("Play")
                    }
                    Button(onClick = onPauseClick) {
                        Text("Pause")
                    }
                }
            }
            is PlayerUiState.Error -> {
                Text(text = uiState.message)
            }
        }
    }
}
