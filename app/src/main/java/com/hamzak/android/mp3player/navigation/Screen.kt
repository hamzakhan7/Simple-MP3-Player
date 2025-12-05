package com.hamzak.android.mp3player.navigation

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Player : Screen("player")
}
