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
package com.jjcampac.eter.ui.station

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjcampac.eter.ui.theme.ThemeMode
import com.jjcampac.eter.ui.theme.ScreenPreviews
import com.jjcampac.eter.ui.theme.EterTheme
import com.jjcampac.eter.ui.PreviewData
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationFormScreen(
    viewModel: StationFormViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StationFormContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onUrlChange = viewModel::onUrlChange,
        onImageUrlChange = viewModel::onImageUrlChange,
        onCountryChange = viewModel::onCountryChange,
        onGenreChange = viewModel::onGenreChange,
        onSave = { viewModel.save(onSaved) },
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * Contenido sin estado del formulario de emisora: recibe el estado y las
 * acciones como parámetros, sin ViewModel ni repositorio.
 *
 * Se expone por separado de [StationFormScreen] para poder previsualizar el
 * alta, la edición, el guardado en curso y el error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StationFormContent(
    state: StationFormUiState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar emisora" else "Añadir emisora") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                // Con el teclado abierto y cinco campos, el formulario puede no caber
                // en pantallas pequeñas: se permite desplazarlo para llegar a Guardar.
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.url,
                onValueChange = onUrlChange,
                label = { Text("URL del stream") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            // Favicon/logo de la emisora: opcional. Se muestra en las listas, en la
            // carátula del reproductor y en la notificación.
            OutlinedTextField(
                value = state.imageUrl,
                onValueChange = onImageUrlChange,
                label = { Text("URL del favicon (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.country,
                onValueChange = onCountryChange,
                label = { Text("País (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.genre,
                onValueChange = onGenreChange,
                label = { Text("Género (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            state.error?.let { err ->
                Text(text = err, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Button(
                    onClick = onSave,
                    enabled = state.isValid && !state.isSaving
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

// ============================================================
// Previsualizaciones
// ============================================================

/** Envoltorio de las previews: tema de la app y fondo, sin ViewModel. */
@Composable
private fun StationFormPreviewContainer(state: StationFormUiState) {
    EterTheme(themeMode = ThemeMode.SISTEMA) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            StationFormContent(
                state = state,
                onNameChange = {},
                onUrlChange = {},
                onImageUrlChange = {},
                onCountryChange = {},
                onGenreChange = {},
                onSave = {},
                onBack = {}
            )
        }
    }
}

/** Alta de emisora: formulario en blanco. */
@ScreenPreviews
@Composable
private fun StationFormAltaPreview() = StationFormPreviewContainer(PreviewData.formularioNuevo)

/** Edición de una emisora existente (campos rellenos). */
@ScreenPreviews
@Composable
private fun StationFormEdicionPreview() = StationFormPreviewContainer(PreviewData.formularioEdicion)

/** Guardando: indicador de progreso y botón deshabilitado. */
@ScreenPreviews
@Composable
private fun StationFormGuardandoPreview() = StationFormPreviewContainer(PreviewData.formularioGuardando)

/** Error al guardar la emisora. */
@ScreenPreviews
@Composable
private fun StationFormErrorPreview() = StationFormPreviewContainer(PreviewData.formularioConError)
