package com.jjcampac.eter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = EterPrimary,
    onPrimary = EterOnPrimary,
    primaryContainer = EterPrimaryContainer,
    onPrimaryContainer = EterOnPrimaryContainer,
    secondary = EterSecondary,
    onSecondary = EterOnSecondary,
    secondaryContainer = EterSecondaryContainer,
    onSecondaryContainer = EterOnSecondaryContainer,
    tertiary = EterTertiary,
    onTertiary = EterOnTertiary,
    tertiaryContainer = EterTertiaryContainer,
    onTertiaryContainer = EterOnTertiaryContainer,
    background = EterBackground,
    onBackground = EterOnBackground,
    surface = EterSurface,
    onSurface = EterOnSurface,
    surfaceVariant = EterSurfaceVariant,
    onSurfaceVariant = EterOnSurfaceVariant,
    error = EterError,
    onError = EterOnError
)

private val DarkColors = darkColorScheme(
    primary = EterPrimaryDark,
    onPrimary = EterOnPrimaryDark,
    primaryContainer = EterPrimaryContainerDark,
    onPrimaryContainer = EterOnPrimaryContainerDark,
    secondary = EterSecondaryDark,
    onSecondary = EterOnSecondaryDark,
    secondaryContainer = EterSecondaryContainerDark,
    onSecondaryContainer = EterOnSecondaryContainerDark,
    tertiary = EterTertiaryDark,
    onTertiary = EterOnTertiaryDark,
    tertiaryContainer = EterTertiaryContainerDark,
    onTertiaryContainer = EterOnTertiaryContainerDark,
    background = EterBackgroundDark,
    onBackground = EterOnBackgroundDark,
    surface = EterSurfaceDark,
    onSurface = EterOnSurfaceDark,
    surfaceVariant = EterSurfaceVariantDark,
    onSurfaceVariant = EterOnSurfaceVariantDark,
    error = EterErrorDark,
    onError = EterOnErrorDark
)

/**
 * Tema de la app. Sigue la preferencia del sistema para claro/oscuro
 * y aplica la tipografía definida en [Type.kt].
 */
@Composable
fun EterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = EterTypography,
        content = content
    )
}
