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
package com.jjcampac.eter.ui.settings

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.BuildConfig
import com.jjcampac.eter.R
import com.jjcampac.eter.di.AppModule
import com.jjcampac.eter.player.SleepTimer
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemeViewModel
import com.jjcampac.eter.util.AppLanguage
import com.jjcampac.eter.util.ReconnectPreferences
import com.jjcampac.eter.ui.theme.ScreenPreviews
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.components.EterTopAppBar
import com.jjcampac.eter.ui.components.EterNavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

/** Secciones desplegables de Ajustes. Solo puede haber una abierta a la vez. */
internal enum class SettingsSection { TEMA, RECONEXION, IDIOMA, DORMIR }

/**
 * Pestaña "Ajustes".
 *
 * El selector de tema es un acordeón desplegable con las opciones Sistema /
 * Claro / Oscuro. Al elegir una opción, el tema se aplica al instante (sin
 * reiniciar la app) gracias al `StateFlow` compartido de [ThemeViewModel] y se
 * persiste en SharedPreferences.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(LocalContext.current))
    val currentTheme by themeViewModel.themeMode.collectAsStateWithLifecycle()

    // Preferencia de reconexión (Fase 5) para informar al usuario tras un corte.
    val context = LocalContext.current
    val reconnectPrefs = remember { ReconnectPreferences(context) }
    var reconnectSeconds by remember { mutableStateOf(reconnectPrefs.getReconnectSeconds()) }

    // Temporizador para dormir (Fase 5).
    val sleepTimer: SleepTimer = remember { AppModule.sleepTimer(context) }
    var sleepMinutes by remember { mutableStateOf(sleepTimer.selectedMinutes()) }

    // Idioma (Fase 5). Los acordeones son excluyentes: solo uno abierto a la vez.
    val currentLanguageCode = remember { AppLanguage.current(context) }
    var openSection by remember { mutableStateOf<SettingsSection?>(null) }

    SettingsScreenContent(
        themeMode = currentTheme,
        reconnectSeconds = reconnectSeconds,
        sleepMinutes = sleepMinutes,
        currentLanguageCode = currentLanguageCode,
        openSection = openSection,
        onSectionToggle = { section ->
            openSection = if (openSection == section) null else section
        },
        onThemeSelected = { mode -> themeViewModel.saveThemeMode(mode) },
        onReconnectSelected = { seconds ->
            reconnectSeconds = seconds
            reconnectPrefs.saveReconnectSeconds(seconds)
        },
        onSleepSelected = { minutes ->
            sleepMinutes = minutes
            if (minutes <= 0) sleepTimer.stop() else sleepTimer.start(minutes)
        },
        onLanguageSelected = { code ->
            if (code != currentLanguageCode) {
                AppLanguage.save(context, code)
                // Recrea la Activity para aplicar el idioma al instante.
                (context as? Activity)?.recreate()
            }
        },
        modifier = modifier
    )
}

/**
 * Contenido sin estado de la pestaña "Ajustes": recibe las preferencias y las
 * acciones como parámetros, sin ViewModel, DataStore, temporizador ni Context.
 *
 * Se expone por separado de [SettingsScreen] para poder previsualizar los
 * acordeones (tema, reconexión, idioma y temporizador) sin dependencias reales.
 */
@Composable
internal fun SettingsScreenContent(
    themeMode: ThemeMode,
    reconnectSeconds: Int,
    sleepMinutes: Int,
    currentLanguageCode: String,
    openSection: SettingsSection?,
    onSectionToggle: (SettingsSection) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onReconnectSelected: (Int) -> Unit,
    onSleepSelected: (Int) -> Unit,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sleepOptions = listOf(
        0 to R.string.sleep_off,
        15 to R.string.sleep_15,
        30 to R.string.sleep_30,
        45 to R.string.sleep_45,
        60 to R.string.sleep_60
    )

    // Nombre visible del idioma actual (AppLanguage es estático: sin Context).
    val currentLanguageName = AppLanguage.options
        .firstOrNull { it.code == currentLanguageCode }
        ?.name ?: AppLanguage.options.first().name
    Column(
        modifier = modifier
            .padding(vertical = 8.dp)
    ) {
        // --- Selector de tema (acordeón) ---
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSectionToggle(SettingsSection.TEMA) },
            headlineContent = {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.settings_theme_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (openSection == SettingsSection.TEMA) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.settings_theme),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        // Opciones de tema expandibles
        AnimatedVisibility(visible = openSection == SettingsSection.TEMA) {
            Column {
                ThemeOption(
                    label = stringResource(R.string.theme_system),
                    description = stringResource(R.string.theme_system_desc),
                    selected = themeMode == ThemeMode.SISTEMA,
                    onClick = { onThemeSelected(ThemeMode.SISTEMA) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ThemeOption(
                    label = stringResource(R.string.theme_light),
                    description = stringResource(R.string.theme_light_desc),
                    selected = themeMode == ThemeMode.CLARO,
                    onClick = { onThemeSelected(ThemeMode.CLARO) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ThemeOption(
                    label = stringResource(R.string.theme_dark),
                    description = stringResource(R.string.theme_dark_desc),
                    selected = themeMode == ThemeMode.OSCURO,
                    onClick = { onThemeSelected(ThemeMode.OSCURO) }
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // --- Reintento de conexión (acordeón, Fase 5) ---
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSectionToggle(SettingsSection.RECONEXION) },
            headlineContent = {
                Text(
                    text = stringResource(R.string.settings_reconnect),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.settings_reconnect_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (openSection == SettingsSection.RECONEXION) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.settings_reconnect),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        AnimatedVisibility(visible = openSection == SettingsSection.RECONEXION) {
            Column {
                ReconnectOption(
                    label = stringResource(R.string.reconnect_10s),
                    selected = reconnectSeconds == 10,
                    onClick = { onReconnectSelected(10) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ReconnectOption(
                    label = stringResource(R.string.reconnect_20s),
                    selected = reconnectSeconds == 20,
                    onClick = { onReconnectSelected(20) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ReconnectOption(
                    label = stringResource(R.string.reconnect_30s),
                    selected = reconnectSeconds == 30,
                    onClick = { onReconnectSelected(30) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ReconnectOption(
                    label = stringResource(R.string.reconnect_unlimited),
                    selected = reconnectSeconds == ReconnectPreferences.UNLIMITED,
                    onClick = { onReconnectSelected(ReconnectPreferences.UNLIMITED) }
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // --- Idioma (acordeón, Fase 5) ---
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSectionToggle(SettingsSection.IDIOMA) },
            headlineContent = {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = currentLanguageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (openSection == SettingsSection.IDIOMA) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.settings_language),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        AnimatedVisibility(visible = openSection == SettingsSection.IDIOMA) {
            Column {
                AppLanguage.options.forEachIndexed { index, option ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    LanguageOption(
                        label = option.name,
                        selected = currentLanguageCode == option.code,
                        onClick = { onLanguageSelected(option.code) }
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // --- Temporizador para dormir (acordeón, Fase 5) ---
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSectionToggle(SettingsSection.DORMIR) },
            headlineContent = {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.settings_sleep_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (openSection == SettingsSection.DORMIR) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.sleep_timer),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        AnimatedVisibility(visible = openSection == SettingsSection.DORMIR) {
            Column {
                sleepOptions.forEachIndexed { index, (minutes, labelRes) ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    SleepOption(
                        label = stringResource(labelRes),
                        selected = sleepMinutes == minutes,
                        onClick = { onSleepSelected(minutes) }
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Acerca de
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            headlineContent = { Text(stringResource(R.string.settings_about)) },
            supportingContent = {
                // La versión se obtiene del gradle (BuildConfig) para que "Acerca de"
                // siempre coincida con el versionName. Debajo va la autoría de la app
                // (texto fijo, no traducible), después una línea separadora y, por
                // último, la descripción traducida.
                Column {
                    Text(
                        stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME) +
                                "\n" + stringResource(R.string.settings_about_author)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(stringResource(R.string.settings_about_desc))
                }
            }
        )
    }
}

/**
 * Fila de una opción de tema dentro del acordeón. Muestra la selección actual
 * con un icono y permite cambiarla con un solo toque.
 */
@Composable
private fun ThemeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(label) },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * Fila de una opción de tiempo de reintento de conexión dentro del acordeón.
 * Muestra la opción seleccionada y permite cambiarla con un toque.
 */
@Composable
private fun ReconnectOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(label) }
    )
}

/**
 * Fila de una opción del temporizador para dormir dentro del acordeón.
 * Muestra la opción seleccionada y permite cambiarla con un toque.
 */
@Composable
private fun SleepOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(label) }
    )
}

/**
 * Fila de una opción de idioma dentro del acordeón. Muestra la opción marcada
 * y, al pulsarla, guarda el idioma y recrea la Activity para aplicarlo.
 */
@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(label) }
    )
}


// ============================================================
// Previsualizaciones
// ============================================================

/**
 * Envoltorio de las previews de la pantalla: tema, Scaffold real (barra
 * superior "Ajustes" y barra de navegación) y contenido sin estado.
 */
@Composable
private fun SettingsScreenPreviewContainer(
    themeMode: ThemeMode = ThemeMode.OSCURO,
    openSection: SettingsSection? = null
) {
    EterTheme(themeMode = ThemeMode.SISTEMA) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { EterTopAppBar(title = stringResource(R.string.tab_settings)) },
            bottomBar = { EterNavigationBar(selectedTab = 2, onTabSelected = {}) }
        ) { innerPadding ->
            SettingsScreenContent(
                themeMode = themeMode,
                reconnectSeconds = 20,
                sleepMinutes = 30,
                currentLanguageCode = "es",
                openSection = openSection,
                onSectionToggle = {},
                onThemeSelected = {},
                onReconnectSelected = {},
                onSleepSelected = {},
                onLanguageSelected = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

/** Ajustes con todos los acordeones cerrados. */
@ScreenPreviews
@Composable
private fun SettingsScreenCerradoPreview() = SettingsScreenPreviewContainer()

/** Ajustes con el selector de tema desplegado. */
@ScreenPreviews
@Composable
private fun SettingsScreenTemaAbiertoPreview() =
    SettingsScreenPreviewContainer(openSection = SettingsSection.TEMA)

/** Ajustes con el temporizador para dormir desplegado. */
@ScreenPreviews
@Composable
private fun SettingsScreenTemporizadorAbiertoPreview() =
    SettingsScreenPreviewContainer(openSection = SettingsSection.DORMIR)
