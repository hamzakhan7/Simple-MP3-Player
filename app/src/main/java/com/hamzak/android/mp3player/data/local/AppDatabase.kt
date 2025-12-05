package com.hamzak.android.mp3player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Song::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
