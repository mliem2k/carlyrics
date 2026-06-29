package com.spotifylyrics.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpotifyDarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBlack,
    primaryContainer = SpotifyGreenDark,
    onPrimaryContainer = SpotifyWhite,
    secondary = SpotifyGreen,
    onSecondary = SpotifyBlack,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = SpotifyMediumGray,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = ErrorRed,
    onError = SpotifyWhite
)

private val SpotifyLightColorScheme = lightColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyWhite,
    primaryContainer = SpotifyGreenLight,
    onPrimaryContainer = SpotifyBlack,
    secondary = SpotifyGreen,
    onSecondary = SpotifyWhite,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = ErrorRed,
    onError = SpotifyWhite
)

@Composable
fun SpotifyLyricsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpotifyDarkColorScheme else SpotifyLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpotifyTypography,
        content = content
    )
}
