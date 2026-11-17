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
package com.jjcampac.eter.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.util.TestData

/**
 * Previsualizaciones de la fila de emisora en los tres estados (normal,
 * seleccionada, pulsada) y en los dos temas (claro y oscuro).
 */
@Composable
private fun StationItemPreviewRow(
    station: Station = TestData.testStation,
    isSelected: Boolean,
    isPressed: Boolean,
    themeMode: ThemeMode,
    onClick: () -> Unit = {}
) {
    val isPressedState = mutableStateOf(isPressed)
    val interactionSource = remember { MutableInteractionSource() }
    EterTheme(themeMode = themeMode) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            StationItemContent(
                station = station,
                isSelected = isSelected,
                interactionSource = interactionSource,
                isPressedState = isPressedState,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Fila normal (oscuro)")
@Composable
fun StationItemPreviewNormalDark() =
    StationItemPreviewRow(isSelected = false, isPressed = false, themeMode = ThemeMode.OSCURO)

@Preview(showBackground = true, name = "Fila seleccionada (oscuro)")
@Composable
fun StationItemPreviewSelectedDark() =
    StationItemPreviewRow(isSelected = true, isPressed = false, themeMode = ThemeMode.OSCURO)

@Preview(showBackground = true, name = "Fila pulsada (oscuro)")
@Composable
fun StationItemPreviewPressedDark() =
    StationItemPreviewRow(isSelected = false, isPressed = true, themeMode = ThemeMode.OSCURO)

@Preview(showBackground = true, name = "Fila normal (claro)")
@Composable
fun StationItemPreviewNormalLight() =
    StationItemPreviewRow(isSelected = false, isPressed = false, themeMode = ThemeMode.CLARO)

@Preview(showBackground = true, name = "Fila seleccionada (claro)")
@Composable
fun StationItemPreviewSelectedLight() =
    StationItemPreviewRow(isSelected = true, isPressed = false, themeMode = ThemeMode.CLARO)

@Preview(showBackground = true, name = "Fila pulsada (claro)")
@Composable
fun StationItemPreviewPressedLight() =
    StationItemPreviewRow(isSelected = false, isPressed = true, themeMode = ThemeMode.CLARO)
