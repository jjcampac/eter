/*
 * Autor: jjcampac
 * IA: cline
 */
package com.jjcampac.eter.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Servidor (mirror) de la API pública de Radio Browser.
 *
 * Se obtienen de `GET /json/servers` y se elige uno aleatorio para distribuir
 * la carga. El esquema (http/https) se deduce del puerto SSL cuando existe.
 */
data class ServerDto(
    @SerializedName("name")
    val name: String?,
    @SerializedName("host")
    val host: String?,
    @SerializedName("port")
    val port: Int?,
    @SerializedName("sslport")
    val sslPort: Int?
)
