package com.jjcampac.eter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colores adicionales que no forman parte de ColorScheme estándar de Material 3.
 * Se resuelven según el tema del sistema oscuro/claro.
 * Usar solo dentro de funciones @Composable.
 */

@Composable
fun EterOutlineColor(): Color {
    return if (isSystemInDarkTheme()) EterOutlineDark else EterOutlineLight
}

@Composable
fun EterSurfaceInputColor(): Color {
    return if (isSystemInDarkTheme()) EterSurfaceInputDark else EterSurfaceInputLight
}

@Composable
fun EterAccentTextColor(): Color {
    return if (isSystemInDarkTheme()) EterAccentTextDark else EterAccentTextLight
}
