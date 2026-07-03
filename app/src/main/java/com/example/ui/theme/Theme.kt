package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CustomLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8)
)

private val CustomDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    background = Color(0xFF030712),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0B1224),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569)
)

private val CustomAmoledColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF2C2C2E)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        "light" -> CustomLightColorScheme
        "dark" -> CustomDarkColorScheme
        "amoled" -> CustomAmoledColorScheme
        else -> {
            if (isSystemInDarkTheme()) CustomDarkColorScheme else CustomLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
