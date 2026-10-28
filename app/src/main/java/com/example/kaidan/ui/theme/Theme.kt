package com.example.kaidan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Палитра Kaidan
val KNight = Color(0xFF0D0D0F)   // фон
val KBlood = Color(0xFFB3001B)   // акцент
val KBone = Color(0xFFF2E8DC)    // текст
val KMist = Color(0xFF7A6A8A)    // второстепенное
val KShadow = Color(0xFF17171C)  // карточки, панели
val KDark = Color(0xFF2A0A10)    // тёмно-красный для плашек

private val KaidanColors = darkColorScheme(
    primary = KBlood,
    onPrimary = KBone,
    background = KNight,
    onBackground = KBone,
    surface = KShadow,
    onSurface = KBone,
    secondary = KMist,
    surfaceVariant = KDark,
    onSurfaceVariant = KBone
)

@Composable
fun KaidanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KaidanColors, content = content)
}
