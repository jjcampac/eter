/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.ui.theme.EterOutlineColor
import com.jjcampac.eter.ui.theme.EterShapeCard
import com.jjcampac.eter.ui.theme.EterSurfaceCardDark
import com.jjcampac.eter.ui.theme.EterSurfaceCardLight
import com.jjcampac.eter.ui.theme.LocalEterDarkTheme

/**
 * Fila de una emisora en las listas de la app.
 *
 * Usa exclusivamente colores de MaterialTheme.colorScheme y formas de ui/theme.
 * No hay colores sueltos en esta pantalla.
 *
 * @param isCurrent  true si es la emisora cargada actualmente en el reproductor.
 * @param isPlaying  true si ademas de ser la actual esta sonando.
 * @param isPaused   true si la emisora actual esta pausada.
 */
@Composable
fun StationItem(
    station: Station,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Las tarjetas usan el MISMO componente y tono que el mini reproductor (MiniPlayer.kt:
    // Surface de color surface con tonalElevation = 12.dp). Así el overlay del surfaceTint
    // (naranja de marca) aclara la tarjeta y deja de confundirse con el fondo de la app.
    // En el estado actual/reproduciendo se mantiene el realce con primaryContainer y borde.
    // El fondo de la tarjeta es SIEMPRE el mismo (surface), tanto para la emisora
    // actual/en reproducción como para el resto. La selección solo se remarca con
    // el borde (EterOutlineColor), que se atenúa en el tema oscuro para no brillar.
    // Usar color de tarjeta específico para tema oscuro: #1f2126
    val hasBorder = isCurrent || isPlaying
    val cardColor = if (LocalEterDarkTheme.current) EterSurfaceCardDark else EterSurfaceCardLight

    Surface(
        // Toda la fila es pulsable: selecciona y reproduce la emisora (y hace
        // aparecer el mini reproductor). Los IconButton de la derecha consumen
        // sus propios toques, así que no interfieren.
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = EterShapeCard,
        color = cardColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 12.dp,
        border = if (hasBorder)
            androidx.compose.foundation.BorderStroke(2.dp, EterOutlineColor())
        else null
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Icono de la emisora con cadena de respaldo (imagen o iniciales). Es decorativo:
            // el nombre de la emisora ya está en el texto contiguo. La emisora actual/en
            // reproducción añade una descripción informativa para accesibilidad.
            EstacionIcono(
                nombre = station.name,
                imageUrl = station.imageUrl,
                size = 40.dp,
                contentDescription = if (station.imageUrl.isNullOrBlank())
                    null else if (isCurrent || isPlaying) "Emisora actual" else null
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))

            // Información textual
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.subtitle.ifEmpty { station.url },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Estado de reproducción (olas animadas o pausa)
            if (isPlaying) {
                AnimatedWavesIcon(
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = "En directo",
                    modifier = Modifier.size(33.dp)
                )
            } else if (isPaused) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Pausa",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))

            // Acciones a la derecha (favorita, editar, borrar)
            Row(
                modifier = Modifier.padding(start = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Favorita toggle
                if (onFavoriteToggle != null) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (station.isFavorite) "Quitar de favoritas" else "Añadir a favoritas",
                            tint = if (station.isFavorite)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (station.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorita",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // Acciones para emisoras manuales
                if (station.isCustom && onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Editar",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                if (station.isCustom && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Borrar",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}


