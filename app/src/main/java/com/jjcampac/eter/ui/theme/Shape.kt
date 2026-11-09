/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Formas según los mock-ups de Eter
// Tarjetas sin redondeo (esquinas rectas) para las listas de emisoras.
val EterShapeCard = RoundedCornerShape(0.dp)
val EterShapePanel = RoundedCornerShape(18.dp)

// Botones de acción: circulares
val EterShapeButton = RoundedCornerShape(24.dp)  // circular aproximado para botones de acción

// Chips: esquinas de 20 dp
val EterShapeChip = RoundedCornerShape(20.dp)

// Icono de iniciales de la emisora: cuadrado con esquinas de 12 dp (mock-ups)
val EterShapeInitials = RoundedCornerShape(12.dp)

// Carátula grande del reproductor: esquinas más redondeadas (mock-up del reproductor)
val EterShapeCover = RoundedCornerShape(24.dp)

// Panel inferior: esquinas superiores de 28 dp
val EterShapeBottomPanel = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
