package com.jjcampac.eter.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.R
import com.jjcampac.eter.ui.theme.EterOutlineColor
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemeViewModel

/**
 * Pestaña "Ajustes".
 *
 * Incluye selector de tema (Sistema / Claro / Oscuro) que se aplica
 * inmediatamente y se persiste en SharedPreferences.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(LocalContext.current))
    val currentTheme by remember { mutableStateOf(themeViewModel.themeMode) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // --- Selector de tema ---
        ListItem(
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
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Opciones de tema
        ThemeOption(
            title = stringResource(R.string.theme_system),
            subtitle = stringResource(R.string.theme_system_desc),
            mode = ThemeMode.SISTEMA,
            selected = currentTheme == ThemeMode.SISTEMA,
            onClick = { themeViewModel.saveThemeMode(ThemeMode.SISTEMA) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        ThemeOption(
            title = stringResource(R.string.theme_light),
            subtitle = stringResource(R.string.theme_light_desc),
            mode = ThemeMode.CLARO,
            selected = currentTheme == ThemeMode.CLARO,
            onClick = { themeViewModel.saveThemeMode(ThemeMode.CLARO) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        ThemeOption(
            title = stringResource(R.string.theme_dark),
            subtitle = stringResource(R.string.theme_dark_desc),
            mode = ThemeMode.OSCURO,
            selected = currentTheme == ThemeMode.OSCURO,
            onClick = { themeViewModel.saveThemeMode(ThemeMode.OSCURO) }
        )

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
 * Fila de opción de tema con botón de selección (radio).
 */
@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (selected) "Seleccionado" else null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.RadioButtonChecked,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
