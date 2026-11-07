package com.jjcampac.eter.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de colores exacta de los mock-ups (tema oscuro)
// Estos colores se definen UNA SOLA VEZ aquí y se mapean a ColorScheme en Theme.kt
// Las pantallas NO deben usar colores sueltos; deben usar MaterialTheme.colorScheme

// background
val EterBackground = Color(0xFF14161A)
val EterOnBackground = Color(0xFFF2F0EA)

// surface (paneles, mini reproductor)
val EterSurface = Color(0xFF1E2229)
val EterOnSurface = Color(0xFFF2F0EA)

// surfaceInput (buscador)
val EterSurfaceInput = Color(0xFF1C2027)

// surfaceVariant (logos, carátula, divisores)
val EterSurfaceVariant = Color(0xFF232730)
val EterOnSurfaceVariant = Color(0xFFA8AAB0)

// outline (bordes)
val EterOutline = Color(0xFF2E333D)

// primary (acento) y onPrimary
val EterPrimary = Color(0xFFF2A33A)
val EterOnPrimary = Color(0xFF14161A)

// error (usado para borrar, etc. - derivado de onBackground para mantener contraste)
val EterError = Color(0xFFF2A33A)
val EterOnError = Color(0xFF14161A)
