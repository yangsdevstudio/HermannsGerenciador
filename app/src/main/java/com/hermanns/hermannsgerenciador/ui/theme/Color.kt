package com.hermanns.hermannsgerenciador.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Home

// Expiry indicator colors
val ExpiryRed = Color(0xFFB00020)
val ExpiryOrange = Color(0xFFFFA000)
val ExpiryYellow = Color(0xFFFFD300)
val ExpiryLime = Color(0xFFBBDB44)
val ExpiryGreen = Color(0xFF2E7D32)
val ExpiryTextRed = Color(0xFFFF6B6A)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00695C),
    secondary = Color(0xFF26A69A),
    tertiary = Color(0xFFFFA726),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64FFDA),
    secondary = Color(0xFF009688),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)