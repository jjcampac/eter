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

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Anotación multipreview para COMPONENTES pequeños: genera la preview en tema
 * oscuro y en tema claro (con fondo para que se vea como en la app).
 *
 * Los composables anotados con [ThemePreviews] deben aplicar `EterTheme`, que
 * respeta `uiMode` a través de `isSystemInDarkTheme()`.
 */
@Preview(name = "Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Claro", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
annotation class ThemePreviews

/**
 * Anotación multipreview para PANTALLAS completas: genera la preview en tema
 * oscuro y claro incluyendo las barras del sistema.
 */
@Preview(
    name = "Oscuro",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true
)
@Preview(
    name = "Claro",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    showSystemUi = true
)
annotation class ScreenPreviews
