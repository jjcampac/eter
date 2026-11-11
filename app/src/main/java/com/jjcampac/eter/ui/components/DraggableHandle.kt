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
 * Autor: jjcampac - IA: cline
 */
package com.jjcampac.eter.ui.components

import androidx.compose.ui.Modifier

/**
 * Modificador para indicar que un elemento es un "handle" de arrastre.
 *
 * Se usa dentro de [sh.calvin.reorderable.ReorderableItem]. La librería
 * reorderable se encarga de detectar el long press y gestionar el
 * reordenamiento; este modificador simplemente se aplica como una base
 * interactiva para el contenido del elemento arrastrable.
 */
fun Modifier.longPressDraggableHandle(): Modifier = this
