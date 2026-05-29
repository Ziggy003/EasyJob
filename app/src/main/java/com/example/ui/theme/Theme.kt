package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Laranja,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC24E1A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE8642A),
    onSecondary = Color.Black,
    tertiary = Color(0xFFC8A97A),
    onTertiary = Color.Black,
    background = Color(0xFF080706), // Deep premium Obsidian Black
    onBackground = Color(0xFFECEAE6), // Crisp warm silver text
    surface = Color(0xFF141210), // Basalt slate container (shadcn/ui dark style)
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1916), // Accent card/button backgrounds
    onSurfaceVariant = Color(0xFFECEAE6),
    outline = Color(0x33E8642A) // Glowing orange outline tint
)

private val LightColorScheme = lightColorScheme(
    primary = Laranja,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBECE5), // Soft pastel orange container
    onPrimaryContainer = Color(0xFFC24E1A),
    secondary = Color(0xFF3D2B1F), // Earthy dark brown
    onSecondary = Color.White,
    tertiary = Color(0xFFC8A97A),
    onTertiary = Color.White,
    background = Color(0xFFFAF9F6), // Warm Ivory surface (not generic dull gray)
    onBackground = Color(0xFF1C1916), // BASALT DARK TEXT
    surface = Color.White,
    onSurface = Color(0xFF1C1916),
    surfaceVariant = Color(0xFFF2EFEA), // Soft warm grey
    onSurfaceVariant = Color(0xFF3D2B1F),
    outline = Color(0xFFE6E2DC) // Clean slate-cream outline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
