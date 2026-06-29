package com.mliem.carlyrics.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mliem.carlyrics.presentation.companion.CompanionLyricsScreen
import com.mliem.carlyrics.presentation.lyricsmanager.LyricsManagerScreen
import com.mliem.carlyrics.presentation.main.MainScreen
import com.mliem.carlyrics.presentation.settings.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val COMPANION = "companion"
    const val LYRICS_MANAGER = "lyrics_manager"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {

        composable(Routes.MAIN) {
            MainScreen(
                viewModel = hiltViewModel(),
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToLyrics = { navController.navigate(Routes.COMPANION) },
                onNavigateToLyricsManager = { navController.navigate(Routes.LYRICS_MANAGER) }
            )
        }

        composable(Routes.COMPANION) {
            CompanionLyricsScreen()
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
