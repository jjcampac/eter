package com.jjcampac.eter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Paleta de colores de los mock-ups mapeada a ColorScheme de Material 3 (tema oscuro)
// Nota: outline no es parte de ColorScheme; se usa EterOutline directamente donde se necesite bordes
private val DarkColors = darkColorScheme(
    primary = EterPrimary,
    onPrimary = EterOnPrimary,
    primaryContainer = EterSurface,
    onPrimaryContainer = EterOnSurface,
    secondary = EterPrimary,
    onSecondary = EterOnPrimary,
    secondaryContainer = EterSurface,
    onSecondaryContainer = EterOnSurface,
    tertiary = EterPrimary,
    onTertiary = EterOnPrimary,
    tertiaryContainer = EterSurface,
    onTertiaryContainer = EterOnSurface,
    background = EterBackground,
    onBackground = EterOnBackground,
    surface = EterSurface,
    onSurface = EterOnSurface,
    surfaceVariant = EterSurfaceVariant,
    onSurfaceVariant = EterOnSurfaceVariant,
    error = EterError,
    onError = EterOnError
)

/**
 * Tema de la app. Solo tema oscuro (según mock-ups).
 * Aplica la tipografía definida en [Type.kt].
 */
@Composable
fun EterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = EterTypography,
        content = content
    )
}
