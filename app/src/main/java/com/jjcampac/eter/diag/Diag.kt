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
 *
 * Instrumentación TEMPORAL de diagnóstico (Paso 1).
 * Se retira cuando se resuelva el fallo de reproducción en segundo plano.
 */
package com.jjcampac.eter.diag

import android.os.Process
import android.util.Log

/** Tag único de los logs temporales de diagnóstico. */
const val DIAG_TAG = "EterDiag"

/**
 * Escribe un log de diagnóstico con el pid del proceso y la identidad de los
 * objetos indicados (servicio, sesión, reproductor, etc.).
 *
 * El tag [DIAG_TAG] permite filtrar con `adb logcat -s EterDiag`.
 */
fun diag(mensaje: String, vararg objetos: Any?) {
    val hashes = objetos.joinToString(" ") { obj ->
        if (obj == null) "obj=null" else "obj=${System.identityHashCode(obj)}"
    }
    Log.d(
        DIAG_TAG,
        "pid=${Process.myPid()} thread=${Thread.currentThread().name} $mensaje $hashes"
    )
}
