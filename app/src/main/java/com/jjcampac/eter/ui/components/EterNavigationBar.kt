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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjcampac.eter.R
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemePreviews

/** Pestañas de la barra de navegación inferior, en el orden en que se muestran. */
object EterTabs {
    const val FAVORITAS = 0
    const val ESTACIONES = 1
    const val AJUSTES = 2
}

/**
 * Barra superior de la app: el nombre de la app en versalitas sobre el título
 * de la pestaña o pantalla visible.
 *
 * @param title título ya traducido de la pantalla actual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EterTopAppBar(title: String, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                // Nombre de la app sobre el título de cada pantalla.
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(title)
            }
        }
    )
}

/**
 * Barra de navegación inferior con las tres pestañas de la app.
 *
 * @param selectedTab índice de la pestaña activa (ver [EterTabs]).
 * @param onTabSelected se invoca con el índice de la pestaña pulsada.
 */
@Composable
fun EterNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = selectedTab == EterTabs.FAVORITAS,
            onClick = { onTabSelected(EterTabs.FAVORITAS) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_favorites)) }
        )
        NavigationBarItem(
            selected = selectedTab == EterTabs.ESTACIONES,
            onClick = { onTabSelected(EterTabs.ESTACIONES) },
            icon = { Icon(Icons.Filled.Radio, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_stations)) }
        )
        NavigationBarItem(
            selected = selectedTab == EterTabs.AJUSTES,
            onClick = { onTabSelected(EterTabs.AJUSTES) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_settings)) }
        )
    }
}

// ============================================================
// Previsualizaciones
// ============================================================

/** Barra de navegación con la pestaña "Favoritas" activa. */
@ThemePreviews
@Composable
private fun EterNavigationBarPreview() {
    EterTheme(themeMode = ThemeMode.SISTEMA) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EterNavigationBar(
                selectedTab = EterTabs.FAVORITAS,
                onTabSelected = {}
            )
        }
    }
}

/** Barra superior con el título de la pestaña "Estaciones". */
@ThemePreviews
@Composable
private fun EterTopAppBarPreview() {
    EterTheme(themeMode = ThemeMode.SISTEMA) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EterTopAppBar(title = stringResource(R.string.all_stations))
        }
    }
}