package com.hamzak.android.mp3player.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String
)
