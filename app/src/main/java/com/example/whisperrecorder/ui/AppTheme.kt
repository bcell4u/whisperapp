package com.example.whisperrecorder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF6F4BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E0FF),
    onPrimaryContainer = Color(0xFF24105F),
    secondary = Color(0xFF675A7A),
    tertiary = Color(0xFF9C416C),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF1ECF5),
    error = Color(0xFFBA1A1A)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFCABEFF),
    onPrimary = Color(0xFF3A208F),
    primaryContainer = Color(0xFF5136B7),
    onPrimaryContainer = Color(0xFFE8E0FF),
    secondary = Color(0xFFD1C2DE),
    tertiary = Color(0xFFFFB0D0)
)

@Composable
fun WhisperRecorderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
