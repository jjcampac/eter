/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.ui.player

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.jjcampac.eter.R
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.player.SleepTimer
import com.jjcampac.eter.ui.components.AnimatedWavesIcon
import com.jjcampac.eter.ui.components.CaratulaStream
import com.jjcampac.eter.ui.theme.EterCoverSize
import com.jjcampac.eter.ui.theme.EterPlayButtonSize
import com.jjcampac.eter.ui.theme.EterShapeCover
import kotlin.math.ceil

private const val TAG = "PlayerScreen"

/**
 * Pantalla del reproductor a pantalla completa (rediseño según mock-up).
 *
 * Distribución vertical:
 *  1. Barra superior: volver · estado breve ("En directo") · corazón de favorita.
 *  2. Carátula grande (carátula del stream o icono de la emisora con su respaldo).
 *  3. Nombre de la emisora y debajo los metadatos ICY (artista – título) o el estado.
 *  4. Controles: favorita anterior · play/pausa · favorita siguiente.
 *  5. Botón del temporizador para dormir, que abre el selector de minutos.
 *
 * El estado viene del [PlayerViewModel] compartido, así que es coherente con el
 * mini reproductor y con los controles del sistema. Se mantienen los colores del
 * tema de la app (acento naranja y fondos/textos del tema claro u oscuro).
 *
 * @param onToggleFavorite Marca/desmarca como favorita la emisora mostrada. Si es
 *   `null` el corazón no se muestra.
 * @param canSkipPrevious `true` si se puede saltar a la favorita anterior (hay
 *   favoritas y la emisora actual no es la primera de la lista).
 * @param canSkipNext `true` si se puede saltar a la favorita siguiente (hay
 *   favoritas y la emisora actual no es la última de la lista).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    station: Station,
    playerViewModel: PlayerViewModel,
    sleepTimer: SleepTimer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((Station) -> Unit)? = null,
    canSkipPrevious: Boolean = true,
    canSkipNext: Boolean = true
) {
    Log.d(TAG, "PlayerScreen() station=${station.name} url=${station.url}")
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val sleepTimerState by sleepTimer.state.collectAsStateWithLifecycle()
    var showTimerDialog by rememberSaveable { mutableStateOf(false) }

    // La emisora mostrada es la que está cargada en el reproductor: al saltar a la
    // anterior/siguiente (o cambiar desde la notificación) la pantalla se actualiza
    // aunque el id con el que se abrió el detalle siga siendo el mismo.
    val displayedStation = playerState.currentStation ?: station

    val isThisStation = playerState.currentStation?.url == displayedStation.url
    val isPlaying = isThisStation && playerState.status == PlaybackStatus.PLAYING
    val isError = isThisStation && playerState.status == PlaybackStatus.ERROR

    // Estado breve de la barra superior (el mock-up muestra "En directo").
    val topStatusText = when {
        !isThisStation -> stringResource(R.string.radio_live)
        playerState.status == PlaybackStatus.PLAYING -> stringResource(R.string.radio_live)
        playerState.status == PlaybackStatus.PAUSED -> stringResource(R.string.radio_paused)
        playerState.status == PlaybackStatus.LOADING -> stringResource(R.string.buffering)
        else -> stringResource(R.string.radio_live)
    }

    // Segunda línea: metadatos ICY (artista – título) o estado de la emisora.
    val subtitleText = when {
        isError -> playerState.errorMessage ?: stringResource(R.string.error_unknown)
        isThisStation && !playerState.metadataTitle.isNullOrBlank() -> playerState.metadataTitle!!
        isThisStation && playerState.status == PlaybackStatus.LOADING ->
            stringResource(R.string.buffering)
        isThisStation && playerState.status == PlaybackStatus.IDLE ->
            stringResource(R.string.tap_to_play)
        displayedStation.subtitle.isNotEmpty() -> displayedStation.subtitle
        else -> stringResource(R.string.metadata_empty)
    }
    val subtitleColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. Barra superior: volver · estado · favorita ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }

                Text(
                    text = topStatusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = { onToggleFavorite.invoke(displayedStation) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = if (displayedStation.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = stringResource(
                                if (displayedStation.isFavorite) R.string.favorite_remove
                                else R.string.favorite_add
                            ),
                            tint = if (displayedStation.isFavorite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // --- 2. Carátula: la del stream si existe; si no, icono de la emisora ---
            CaratulaStream(
                nombre = displayedStation.name,
                imageUrl = displayedStation.imageUrl,
                streamArtworkData = playerState.streamArtworkData,
                streamArtworkUri = playerState.streamArtworkUri,
                size = EterCoverSize,
                shape = EterShapeCover
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- 3. Nombre de la emisora y metadatos (artista – título) ---
            Text(
                text = displayedStation.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ondas animadas entre el país y los controles. El hueco está SIEMPRE
            // reservado (aunque no haya reproducción) para que la vista no se mueva
            // al reproducir o pausar: solo cambia la opacidad del icono.
            val wavesAlpha by animateFloatAsState(
                targetValue = if (isPlaying) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "wavesAlpha"
            )
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedWavesIcon(
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = if (isPlaying) {
                        stringResource(R.string.radio_live)
                    } else {
                        null
                    },
                    modifier = Modifier
                        .size(96.dp)
                        .alpha(wavesAlpha)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- 4. Controles: anterior · play/pausa · siguiente ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = { playerViewModel.prevFavorite() },
                    enabled = canSkipPrevious,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.skip_previous),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                FilledIconButton(
                    onClick = { playerViewModel.togglePlay(displayedStation) },
                    modifier = Modifier.size(EterPlayButtonSize)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.pause else R.string.play
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                OutlinedIconButton(
                    onClick = { playerViewModel.nextFavorite() },
                    enabled = canSkipNext,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.skip_next),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- 5. Temporizador para dormir ---
            val remainingMinutes = if (sleepTimerState.active) {
                ceil(sleepTimerState.remainingMillis / 60_000.0).toInt().coerceAtLeast(1)
            } else {
                0
            }
            OutlinedButton(
                onClick = { showTimerDialog = true },
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text = if (sleepTimerState.active) {
                        stringResource(R.string.timer_button_active, remainingMinutes)
                    } else {
                        stringResource(R.string.timer_button)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Selector de minutos del temporizador (mismas opciones que en Ajustes).
    if (showTimerDialog) {
        TimerDialog(sleepTimer = sleepTimer, onDismiss = { showTimerDialog = false })
    }
}

/**
 * Diálogo de selección del temporizador para dormir.
 *
 * Al tocar una opción se aplica al momento (Desactivado detiene el temporizador;
 * el resto lo inicia con esos minutos) y se cierra. "Aceptar" solo cierra.
 */
@Composable
private fun TimerDialog(
    sleepTimer: SleepTimer,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to R.string.sleep_off,
        15 to R.string.sleep_15,
        30 to R.string.sleep_30,
        45 to R.string.sleep_45,
        60 to R.string.sleep_60
    )
    val selectedMinutes = sleepTimer.selectedMinutes()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.sleep_timer)) },
        text = {
            Column {
                options.forEach { (minutes, labelRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedMinutes == minutes,
                                role = Role.RadioButton,
                                onClick = {
                                    if (minutes == 0) sleepTimer.stop() else sleepTimer.start(minutes)
                                    onDismiss()
                                }
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMinutes == minutes,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.ok))
            }
        }
    )
}
