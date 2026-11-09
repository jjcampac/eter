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
