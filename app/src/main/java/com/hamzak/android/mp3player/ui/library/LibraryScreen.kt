package com.hamzak.android.mp3player.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.hamzak.android.mp3player.data.local.Song

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit
) {
    val songs by viewModel.songs.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.addSong(it) }
        }
    )

    LibraryScreen(
        songs = songs,
        onAddSongClick = { launcher.launch(arrayOf("audio/mpeg")) },
        onSongClick = onSongClick
    )
}

@Composable
private fun LibraryScreen(
    songs: List<Song>,
    onAddSongClick: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (songs.isEmpty()) {
            Text(text = "No songs in library")
        } else {
            LazyColumn {
                items(songs) { song ->
                    Text(
                        text = "${song.title} - ${song.artist}",
                        modifier = Modifier.clickable { onSongClick(song) }
                    )
                }
            }
        }
        Button(onClick = onAddSongClick, modifier = Modifier.align(Alignment.BottomCenter)) {
            Text("Add Song")
        }
    }
}
