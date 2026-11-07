package com.jjcampac.eter.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.R
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ThemeViewModel

/**
 * Pestaña "Ajustes".
 *
 * Incluye selector de tema (Sistema / Claro / Oscuro) mediante un desplegable
 * anclado al selector que se aplica inmediatamente y se persiste en SharedPreferences.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(LocalContext.current))
    var currentTheme by remember { mutableStateOf(themeViewModel.themeMode) }
    var themeMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // --- Selector de tema (desplegable anclado) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
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
                    IconButton(onClick = { themeMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Abrir selector de tema",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Desplegable de opciones de tema (anclado al selector)
            DropdownMenu(
                expanded = themeMenuOpen,
                onDismissRequest = { themeMenuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.theme_system)) },
                    onClick = {
                        themeMenuOpen = false
                        themeViewModel.saveThemeMode(ThemeMode.SISTEMA)
                        currentTheme = ThemeMode.SISTEMA
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.theme_light)) },
                    onClick = {
                        themeMenuOpen = false
                        themeViewModel.saveThemeMode(ThemeMode.CLARO)
                        currentTheme = ThemeMode.CLARO
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.theme_dark)) },
                    onClick = {
                        themeMenuOpen = false
                        themeViewModel.saveThemeMode(ThemeMode.OSCURO)
                        currentTheme = ThemeMode.OSCURO
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
