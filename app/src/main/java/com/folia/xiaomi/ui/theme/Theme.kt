package com.folia.xiaomi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Folia 的"叶"之青色系
val FoliaPrimary = Color(0xFF1B9E55)
val FoliaPrimaryDark = Color(0xFF22C55E)
val FoliaSurface = Color(0xFFF4FAF6)
val FoliaSurfaceDark = Color(0xFF0B1410)

private val LightColors = lightColorScheme(
    primary = FoliaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F4C7),
    onPrimaryContainer = Color(0xFF05310F),
    secondary = Color(0xFF3B6B4B),
    background = FoliaSurface,
    surface = FoliaSurface,
)

private val DarkColors = darkColorScheme(
    primary = FoliaPrimaryDark,
    onPrimary = Color(0xFF062812),
    primaryContainer = Color(0xFF0E4D23),
    onPrimaryContainer = Color(0xFFB7F4C7),
    secondary = Color(0xFF8FCFA0),
    background = FoliaSurfaceDark,
    surface = FoliaSurfaceDark,
)

@Composable
fun FoliaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
