package com.circuitsense.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = NeonAmber,
    tertiary = ResistorOrange,
    background = SpaceDark,
    surface = CardDark,
    onPrimary = Color(0xFF0F111A),
    onSecondary = Color(0xFF0F111A),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun CircuitSenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
