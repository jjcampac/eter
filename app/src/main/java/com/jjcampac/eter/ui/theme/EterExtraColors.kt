/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colores adicionales que no forman parte de ColorScheme estándar de Material 3.
 * Se resuelven según el tema REAL aplicado (LocalEterDarkTheme), que respeta el
 * ThemeMode elegido por el usuario (SISTEMA/CLARO/OSCURO), no solo el del sistema.
 * Usar solo dentro de funciones @Composable.
 */

@Composable
fun EterOutlineColor(): Color {
    return if (LocalEterDarkTheme.current) EterOutlineDark else EterOutlineLight
}

@Composable
fun EterSurfaceInputColor(): Color {
    return if (LocalEterDarkTheme.current) EterSurfaceInputDark else EterSurfaceInputLight
}

@Composable
fun EterAccentTextColor(): Color {
    return if (LocalEterDarkTheme.current) EterAccentTextDark else EterAccentTextLight
}
