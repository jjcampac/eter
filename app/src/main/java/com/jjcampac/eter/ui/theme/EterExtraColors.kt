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
