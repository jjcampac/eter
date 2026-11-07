package com.jjcampac.eter.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jjcampac.eter.R

/**
 * Pestaña "Ajustes".
 *
 * Estructura visual básica de ajustes. El temporizador de sueño y el selector
 * de tema se añadirán en la Fase 5; de momento se muestran como opciones
 * informativas para que la navegación por pestañas quede completa.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // Ajustes aún no implementados (Fase 5) -> se muestran así.
        TitleRow(stringResource(R.string.settings_theme), stringResource(R.string.settings_theme_desc))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        TitleRow(stringResource(R.string.settings_language), stringResource(R.string.settings_language_desc))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        TitleRow(stringResource(R.string.sleep_timer), "")

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

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

@Composable
private fun TitleRow(titulo: String, descripcion: String) {
    ListItem(
        headlineContent = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            if (descripcion.isNotEmpty()) {
                Text(descripcion)
            } else {
                Text(
                    text = stringResource(R.string.settings_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
