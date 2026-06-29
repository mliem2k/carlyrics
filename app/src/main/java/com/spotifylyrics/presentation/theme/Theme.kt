package com.spotifylyrics.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Spotify-inspired theme
 */
private val SpotifyDarkColors = darkColors(
    primary = SpotifyGreen,
    primaryVariant = SpotifyGreenDark,
    secondary = SpotifyGreen,
    secondaryVariant = SpotifyGreenLight,
    background = DarkBackground,
    surface = DarkSurface,
    error = ErrorRed,
    onPrimary = SpotifyBlack,
    onSecondary = SpotifyBlack,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onError = SpotifyWhite
)

private val SpotifyLightColors = lightColors(
    primary = SpotifyGreen,
    primaryVariant = SpotifyGreenDark,
    secondary = SpotifyGreen,
    secondaryVariant = SpotifyGreenDark,
    background = LightBackground,
    surface = LightSurface,
    error = ErrorRed,
    onPrimary = SpotifyWhite,
    onSecondary = SpotifyWhite,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onError = SpotifyWhite
)

private val SpotifyShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(0.dp) // No rounded corners for cards, like Spotify
)

@Composable
fun SpotifyLyricsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) SpotifyDarkColors else SpotifyLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colors = colors,
        typography = SpotifyTypography,
        shapes = SpotifyShapes,
        content = content
    )
}
