/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Formas según los mock-ups de Eter
// Tarjetas y paneles: esquinas de 12-18 dp
val EterShapeCard = RoundedCornerShape(16.dp)
val EterShapePanel = RoundedCornerShape(18.dp)

// Botones de acción: circulares
val EterShapeButton = RoundedCornerShape(24.dp)  // circular aproximado para botones de acción

// Chips: esquinas de 20 dp
val EterShapeChip = RoundedCornerShape(20.dp)

// Panel inferior: esquinas superiores de 28 dp
val EterShapeBottomPanel = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
