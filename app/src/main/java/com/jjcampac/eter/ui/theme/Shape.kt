/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2026 jjcampac
 */

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
