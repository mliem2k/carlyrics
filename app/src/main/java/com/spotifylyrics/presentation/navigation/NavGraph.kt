package com.spotifylyrics.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spotifylyrics.presentation.lyrics.LyricsScreen
import com.spotifylyrics.presentation.lyricsmanager.LyricsManagerScreen
import com.spotifylyrics.presentation.main.MainScreen
import com.spotifylyrics.presentation.settings.SettingsScreen

/**
 * Navigation routes
 */
object Routes {
    const val MAIN = "main"
    const val LYRICS = "lyrics"
    const val LYRICS_MANAGER = "lyrics_manager"
    const val SETTINGS = "settings"
}

/**
 * Main navigation graph
 */
@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                viewModel = hiltViewModel(),
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToLyrics = { navController.navigate(Routes.LYRICS) },
                onNavigateToLyricsManager = { navController.navigate(Routes.LYRICS_MANAGER) }
            )
        }

        composable(Routes.LYRICS) {
            LyricsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LYRICS_MANAGER) {
            LyricsManagerScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
