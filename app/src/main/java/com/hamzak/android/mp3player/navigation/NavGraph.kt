package com.hamzak.android.mp3player.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hamzak.android.mp3player.ui.library.LibraryScreen
import com.hamzak.android.mp3player.ui.player.PlayerScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Library.route) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onSongClick = { song ->
                    navController.navigate("${Screen.Player.route}/${song.id}")
                }
            )
        }
        composable(
            route = "${Screen.Player.route}/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.IntType })
        ) {
            PlayerScreen()
        }
    }
}
