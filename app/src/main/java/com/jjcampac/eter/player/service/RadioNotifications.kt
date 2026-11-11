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
package com.jjcampac.eter.player.service

/**
 * Constantes de la notificación multimedia de Eter.
 *
 * La notificación la construye Media3 (`DefaultMediaNotificationProvider`) a
 * partir de la `MediaSession`, con el estilo multimedia del sistema: tarjeta
 * en la cortina con la carátula/logo de la emisora, el título, la canción en
 * curso, los controles y el selector de salida de audio ("Este teléfono").
 *
 * Aquí solo viven el canal, el identificador y la acción del temporizador para
 * que el servicio y el resto de la app usen los mismos valores.
 */

/** Canal de notificación del reproductor (Android 8.0+). */
const val RADIO_CHANNEL_ID = "com.jjcampac.eter.player"

/** ID de la notificación persistente del reproductor. */
const val RADIO_NOTIFICATION_ID = 1001

/**
 * Acción interna del temporizador para dormir: [com.jjcampac.eter.player.SleepTimer]
 * la envía al servicio cuando se agota el tiempo para detener la reproducción.
 */
const val ACTION_SLEEP_TIMER = "com.jjcampac.eter.ACTION_SLEEP_TIMER"
