package com.lucidera.investigations.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = LucidTeal,
    onPrimary = Color.White,
    primaryContainer = LucidPanel,
    onPrimaryContainer = LucidInk,
    secondary = LucidSlate,
    onSecondary = Color.White,
    secondaryContainer = LucidPanel,
    onSecondaryContainer = LucidInk,
    tertiary = LucidGreen,
    onTertiary = Color.White,
    background = LucidFog,
    onBackground = LucidInk,
    surface = Color.White,
    onSurface = LucidInk,
    surfaceVariant = LucidPanel,
    onSurfaceVariant = LucidInk,
    outline = LucidOutline
)

private val DarkColors = darkColorScheme(
    primary = LucidGreen,
    onPrimary = Color.Black,
    primaryContainer = LucidTeal,
    onPrimaryContainer = LucidFog,
    secondary = LucidSlate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E2C2E),
    onSecondaryContainer = LucidFog,
    tertiary = LucidGreen,
    onTertiary = Color.Black,
    background = Color(0xFF101617),
    onBackground = LucidFog,
    surface = Color(0xFF172123),
    onSurface = LucidFog,
    surfaceVariant = Color(0xFF223033),
    onSurfaceVariant = Color(0xFFD5E3DF),
    outline = Color(0xFF7D9690)
)

@Composable
fun LucidEraTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
