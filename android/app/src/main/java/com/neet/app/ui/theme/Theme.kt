package com.neet.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeetGreen = Color(0xFF1B5E20)
private val NeetGreenLight = Color(0xFF4C8C4A)

private val LightColors = lightColorScheme(
    primary = NeetGreen,
    secondary = NeetGreenLight,
)

private val DarkColors = darkColorScheme(
    primary = NeetGreenLight,
    secondary = NeetGreen,
)

@Composable
fun NeetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
