package com.weather.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iOS Weather–inspired palette
val SkyBlue = Color(0xFF1C8DFF)
val SkyBlueDark = Color(0xFF0E5CB3)
val SkyNight = Color(0xFF0D1B3E)
val SkyNightLight = Color(0xFF1A2F5E)
val CloudGray = Color(0xFFB0C4DE)
val SunYellow = Color(0xFFFFD60A)
val RainBlue = Color(0xFF5AC8FA)
val CardWhite = Color(0x1AFFFFFF)  // lighter, less intrusive card layer
val TextPrimary = Color.White
val TextSecondary = Color(0xF2FFFFFF)

private val WeatherColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    secondary = RainBlue,
    onSecondary = Color.White,
    background = SkyNight,
    onBackground = TextPrimary,
    surface = SkyNightLight,
    onSurface = TextPrimary,
    surfaceVariant = CardWhite,
    onSurfaceVariant = TextPrimary,
)

@Composable
fun WeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WeatherColorScheme,
        typography = Typography(),
        content = content
    )
}
