package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AcademicBlue,
    onPrimary = Color.White,
    primaryContainer = AcademicBlue.copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = AcademicPurple,
    onSecondary = Color.White,
    tertiary = AcademicGreen,
    background = NotionBgDark,
    onBackground = NotionTextPrimaryDark,
    surface = NotionCardDark,
    onSurface = NotionTextPrimaryDark,
    surfaceVariant = NotionSurfaceDark,
    onSurfaceVariant = NotionTextSecondaryDark,
    outline = NotionBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = AcademicPrimary,
    onPrimary = Color.White,
    primaryContainer = AcademicBlueSubtle,
    onPrimaryContainer = AcademicBlue,
    secondary = AcademicBlue,
    onSecondary = Color.White,
    tertiary = AcademicGreen,
    background = NotionBgLight,
    onBackground = NotionTextPrimary,
    surface = NotionCardLight,
    onSurface = NotionTextPrimary,
    surfaceVariant = NotionSurfaceLight,
    onSurfaceVariant = NotionTextSecondary,
    outline = NotionBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Notion aesthetic
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

