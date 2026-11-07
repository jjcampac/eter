package com.jjcampac.eter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================
// ColorScheme de Material 3 para tema claro y oscuro
// Mapeado desde los valores exactos de los mock-ups.
// surfaceInput y accentText no son parte estándar de ColorScheme;
// se exponen como propiedades del tema para usarse en pantallas.
// ============================================================

private val DarkColors = darkColorScheme(
    primary = EterPrimaryDark,
    onPrimary = EterOnPrimaryDark,
    primaryContainer = EterSurfaceDark,
    onPrimaryContainer = EterOnSurfaceDark,
    secondary = EterPrimaryDark,
    onSecondary = EterOnPrimaryDark,
    secondaryContainer = EterSurfaceDark,
    onSecondaryContainer = EterOnSurfaceDark,
    tertiary = EterPrimaryDark,
    onTertiary = EterOnPrimaryDark,
    tertiaryContainer = EterSurfaceDark,
    onTertiaryContainer = EterOnSurfaceDark,
    background = EterBackgroundDark,
    onBackground = EterOnBackgroundDark,
    surface = EterSurfaceDark,
    onSurface = EterOnSurfaceDark,
    surfaceVariant = EterSurfaceVariantDark,
    onSurfaceVariant = EterOnSurfaceVariantDark,
    error = EterErrorDark,
    onError = EterOnErrorDark
)

private val LightColors = lightColorScheme(
    primary = EterPrimaryLight,
    onPrimary = EterOnPrimaryLight,
    primaryContainer = EterSurfaceLight,
    onPrimaryContainer = EterOnSurfaceLight,
    secondary = EterPrimaryLight,
    onSecondary = EterOnPrimaryLight,
    secondaryContainer = EterSurfaceLight,
    onSecondaryContainer = EterOnSurfaceLight,
    tertiary = EterPrimaryLight,
    onTertiary = EterOnPrimaryLight,
    tertiaryContainer = EterSurfaceLight,
    onTertiaryContainer = EterOnSurfaceLight,
    background = EterBackgroundLight,
    onBackground = EterOnBackgroundLight,
    surface = EterSurfaceLight,
    onSurface = EterOnSurfaceLight,
    surfaceVariant = EterSurfaceVariantLight,
    onSurfaceVariant = EterOnSurfaceVariantLight,
    error = EterErrorLight,
    onError = EterOnErrorLight
)

// Colores adicionales que no forman parte de ColorScheme estándar

/**
 * Tema de la app. Respeta el tema del sistema (claro/oscuro) por defecto.
 * Todas las pantallas deben usar MaterialTheme.colorScheme y no colores sueltos.
 */
@Composable
fun EterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = EterTypography,
        content = content
    )
}
