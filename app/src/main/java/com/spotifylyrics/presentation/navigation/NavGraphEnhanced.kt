package com.spotifylyrics.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spotifylyrics.presentation.companion.CompanionLyricsScreen
import com.spotifylyrics.presentation.lyrics.LyricsScreen
import com.spotifylyrics.presentation.lyricsmanager.LyricsManagerScreen
import com.spotifylyrics.presentation.settings.SettingsScreen

/**
 * Enhanced Navigation Graph with Companion View
 */
object NavigationDestinations {
    const val MAIN = "main"
    const val COMPANION = "companion"
    const val LYRICS_MANAGER = "lyrics_manager"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraphEnhanced(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestinations.MAIN
    ) {
        composable(NavigationDestinations.MAIN) {
            LyricsScreen(
                onNavigateToCompanion = {
                    navController.navigate(NavigationDestinations.COMPANION)
                },
                onNavigateToSettings = {
                    navController.navigate(NavigationDestinations.SETTINGS)
                },
                onNavigateToLyricsManager = {
                    navController.navigate(NavigationDestinations.LYRICS_MANAGER)
                }
            )
        }
        
        composable(NavigationDestinations.COMPANION) {
            CompanionLyricsScreen()
        }
        
        composable(NavigationDestinations.LYRICS_MANAGER) {
            LyricsManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(NavigationDestinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
