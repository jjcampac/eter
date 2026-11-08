/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
/**
 * Tema de la app. Respeta el tema del sistema por defecto.
 * Todas las pantallas deben usar MaterialTheme.colorScheme y no colores sueltos.
 */
@Composable
fun EterTheme(
    themeMode: ThemeMode = ThemeMode.SISTEMA,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
        ThemeMode.CLARO -> false
        ThemeMode.OSCURO -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // Barras del sistema: ajusta el contraste de sus iconos al tema REAL de la app
    // (oscuro -> iconos claros; claro -> iconos oscuros). Con enableEdgeToEdge() en
    // MainActivity las barras ya son transparentes y el fondo lo dibuja Compose; aquí
    // solo se decide la apariencia de la hora/wi-fi/batería y de los botones de
    // navegación. Es el único sitio donde se conoce darkTheme y se recompone en vivo
    // ante cambios de tema de la app, del sistema o rotación.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val insetsController = WindowCompat.getInsetsController(
                (view.context as Activity).window, view
            )
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EterTypography,
        content = content
    )
}
