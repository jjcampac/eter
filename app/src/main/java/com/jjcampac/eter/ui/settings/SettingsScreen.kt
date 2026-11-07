package com.jjcampac.eter.ui.settings

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
import com.jjcampac.eter.R
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemeViewModel
import com.jjcampac.eter.util.ReconnectPreferences

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
    var themeExpanded by remember { mutableStateOf(false) }

    // Preferencia de reconexión (Fase 5) para informar al usuario tras un corte.
    val context = LocalContext.current
    val reconnectPrefs = remember { ReconnectPreferences(context) }
    var reconnectSeconds by remember { mutableStateOf(reconnectPrefs.getReconnectSeconds()) }
    var reconnectExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(vertical = 8.dp)
    ) {
        // --- Selector de tema (acordeón) ---
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { themeExpanded = !themeExpanded },
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
                    imageVector = if (themeExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.settings_theme),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        // Opciones de tema expandibles
        AnimatedVisibility(visible = themeExpanded) {
            Column {
                ThemeOption(
                    label = stringResource(R.string.theme_system),
                    description = stringResource(R.string.theme_system_desc),
                    selected = currentTheme == ThemeMode.SISTEMA,
                    onClick = { themeViewModel.saveThemeMode(ThemeMode.SISTEMA) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ThemeOption(
                    label = stringResource(R.string.theme_light),
                    description = stringResource(R.string.theme_light_desc),
                    selected = currentTheme == ThemeMode.CLARO,
                    onClick = { themeViewModel.saveThemeMode(ThemeMode.CLARO) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ThemeOption(
                    label = stringResource(R.string.theme_dark),
                    description = stringResource(R.string.theme_dark_desc),
                    selected = currentTheme == ThemeMode.OSCURO,
                    onClick = { themeViewModel.saveThemeMode(ThemeMode.OSCURO) }
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // --- Reintento de conexión (acordeón, Fase 5) ---
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { reconnectExpanded = !reconnectExpanded },
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
                    imageVector = if (reconnectExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.settings_reconnect),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        AnimatedVisibility(visible = reconnectExpanded) {
            Column {
                ReconnectOption(
                    label = stringResource(R.string.reconnect_10s),
                    selected = reconnectSeconds == 10,
                    onClick = {
                        reconnectSeconds = 10
                        reconnectPrefs.saveReconnectSeconds(10)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ReconnectOption(
                    label = stringResource(R.string.reconnect_20s),
                    selected = reconnectSeconds == 20,
                    onClick = {
                        reconnectSeconds = 20
                        reconnectPrefs.saveReconnectSeconds(20)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ReconnectOption(
                    label = stringResource(R.string.reconnect_30s),
                    selected = reconnectSeconds == 30,
                    onClick = {
                        reconnectSeconds = 30
                        reconnectPrefs.saveReconnectSeconds(30)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                ReconnectOption(
                    label = stringResource(R.string.reconnect_unlimited),
                    selected = reconnectSeconds == ReconnectPreferences.UNLIMITED,
                    onClick = {
                        reconnectSeconds = ReconnectPreferences.UNLIMITED
                        reconnectPrefs.saveReconnectSeconds(ReconnectPreferences.UNLIMITED)
                    }
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Idioma (pendiente Fase 5)
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.settings_language_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Temporizador (pendiente Fase 5)
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.settings_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

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
            supportingContent = { Text(stringResource(R.string.settings_about_desc)) }
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
