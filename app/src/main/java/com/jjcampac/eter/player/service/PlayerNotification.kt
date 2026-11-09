/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.player.service

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jjcampac.eter.R
import com.jjcampac.eter.domain.model.Station
import com.jjcampac.eter.player.PlaybackStatus
import com.jjcampac.eter.MainActivity
import kotlin.math.max

private const val TAG = "PlayerNotification"

/** Canal de notificación para el reproductor (Android 8.0+). */
const val NOTIFICATION_CHANNEL_ID = "com.jjcampac.eter.player"
const val NOTIFICATION_CHANNEL_NAME = "Reproductor de radio"

/** ID de la notificación persistente. */
const val NOTIFICATION_ID = 1001

/** Acciones de la notificación. */
const val ACTION_PLAY_PAUSE = "com.jjcampac.eter.ACTION_PLAY_PAUSE"
const val ACTION_NEXT = "com.jjcampac.eter.ACTION_NEXT"
const val ACTION_PREVIOUS = "com.jjcampac.eter.ACTION_PREVIOUS"
const val ACTION_STOP = "com.jjcampac.eter.ACTION_STOP"

/** Acción interna del temporizador para dormir (Fase 5). */
const val ACTION_SLEEP_TIMER = "com.jjcampac.eter.ACTION_SLEEP_TIMER"

/**
 * Crea el canal de notificación una sola vez (Android 8.0+).
 */
fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción de radio en segundo plano"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}

/**
 * Construye la notificación persistente del reproductor con controles de
 * play/pausa y siguiente/anterior.
 *
 * Si el stream entrega carátula embebida ([artworkData]) se muestra como imagen
 * grande (big picture). No se sustituye el icono de la emisora en las listas;
 * esto solo afecta a la notificación. El artwork llega como bytes de Media3
 * (`MediaMetadata.artworkData`), sin peticiones de red adicionales.
 */
fun buildNotification(
    context: Context,
    station: Station?,
    status: PlaybackStatus,
    metadataTitle: String?,
    artworkData: ByteArray? = null
): android.app.Notification {
    val isPlaying = status == PlaybackStatus.PLAYING

    // Intent para abrir la app al tocar la notificación.
    val activityIntent = Intent(context, MainActivity::class.java)
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val contentIntent = PendingIntent.getActivity(
        context,
        0,
        activityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Play / Pausa
    val playPauseIntent = PendingIntent.getService(
        context,
        1,
        Intent(context, RadioMediaSessionService::class.java).setAction(ACTION_PLAY_PAUSE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Siguiente / Anterior (placeholders para Fase 3)
    val nextIntent = PendingIntent.getService(
        context,
        2,
        Intent(context, RadioMediaSessionService::class.java).setAction(ACTION_NEXT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val previousIntent = PendingIntent.getService(
        context,
        3,
        Intent(context, RadioMediaSessionService::class.java).setAction(ACTION_PREVIOUS),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Detener
    val stopIntent = PendingIntent.getService(
        context,
        4,
        Intent(context, RadioMediaSessionService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(station?.name ?: context.getString(R.string.app_name))
        .setContentText(metadataTitle ?: station?.subtitle ?: context.getString(R.string.buffering))
        .setSubText(if (isPlaying) context.getString(R.string.radio_live) else context.getString(R.string.radio_paused))
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        .setShowWhen(false)
        .setContentIntent(contentIntent)
        .addAction(
            NotificationCompat.Action(
                R.drawable.ic_notification_previous,
                context.getString(R.string.skip_previous),
                previousIntent
            )
        )
        .addAction(
            NotificationCompat.Action(
                if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play,
                if (isPlaying) context.getString(R.string.pause) else context.getString(R.string.play),
                playPauseIntent
            )
        )
        .addAction(
            NotificationCompat.Action(
                R.drawable.ic_notification_next,
                context.getString(R.string.skip_next),
                nextIntent
            )
        )
        .addAction(
            NotificationCompat.Action(
                R.drawable.ic_notification_play,
                context.getString(R.string.stop),
                stopIntent
            )
        )

    // Carátula embebida del stream: imagen grande + icono de estado (paso c).
    // Decodificación acotada a 512 px por lado para no disparar la memoria.
    if (artworkData != null) {
        decodeArtwork(artworkData)?.let { artwork ->
            builder
                .setLargeIcon(artwork)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(artwork)
                        .bigLargeIcon(null as Bitmap?)
                )
        }
    }

    return builder.build()
}

/**
 * Decodifica la carátula del stream acotando el tamaño (muestreo) para evitar
 * consumir demasiada memoria o ralentizar el hilo que reconstruye la
 * notificación (que es el principal). Devuelve `null` si no se puede decodificar.
 */
private fun decodeArtwork(data: ByteArray): Bitmap? = try {
    // Primer pase: dimensiones reales sin decodificar la imagen completa.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
    val maxSide = max(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (maxSide / (sample * 2) >= 512) sample *= 2

    // Segundo pase: imagen real con muestreo.
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeByteArray(data, 0, data.size, options)
} catch (e: Exception) {
    Log.w(TAG, "No se pudo decodificar la carátula: ${e.message}")
    null
}
