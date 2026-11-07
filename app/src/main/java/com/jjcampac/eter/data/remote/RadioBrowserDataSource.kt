package com.jjcampac.eter.data.remote

import android.util.Log
import com.jjcampac.eter.data.remote.dto.StationDto
import com.jjcampac.eter.domain.model.Station
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

private const val TAG = "RadioBrowser"
private const val USER_AGENT = "Eter/1.0 (jjcapa@gmail.com)"
private const val FALLBACK_MIRROR = "https://de1.api.radio-browser.info/"
private const val DISCOVERY_MIRROR = "https://all.api.radio-browser.info/"

/**
 * Acceso a la API pública de Radio Browser.
 *
 * - Elige un mirror aleatorio la primera vez (`json/servers`) con fallback a
 *   [FALLBACK_MIRROR] si no se puede obtener la lista.
 * - Configura Retrofit con el [USER_AGENT] de la app (requisito de la API).
 * - Convierte los DTO en emisoras de dominio listas para reproducir.
 *
 * Operaciones suspend: siempre se llaman desde corrutinas (fuera del hilo principal).
 */
class RadioBrowserDataSource {

    // Mirror ya resuelto para no repetir la consulta en cada búsqueda.
    @Volatile
    private var mirrorUrl: String? = null

    /** Construye el cliente Retrofit apuntando a un mirror dado. */
    private fun buildApi(baseUrl: String): RadioBrowserApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                )
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create()
    }

    /** Devuelve la API usando el mirror cacheado, resolviéndolo si es la primera vez. */
    private suspend fun api(): RadioBrowserApi {
        val cached = mirrorUrl
        if (cached != null) return buildApi(cached)
        val resolved = resolveMirror()
        mirrorUrl = resolved
        Log.d(TAG, "mirror seleccionado: $resolved")
        return buildApi(resolved)
    }

    /**
     * Obtiene la lista de mirrors y elige uno aleatorio.
     * Ante cualquier error (sin red, mirror caído...) usa el fallback por defecto.
     */
    private suspend fun resolveMirror(): String {
        return try {
            val servers = buildApi(DISCOVERY_MIRROR).servers()
            val candidate = servers.filter { !it.host.isNullOrBlank() }.randomOrNull()
            candidate?.let { server ->
                val host = server.host ?: return@let FALLBACK_MIRROR
                val useSsl = server.sslPort != null && server.sslPort > 0
                val port = if (useSsl) server.sslPort else server.port
                val scheme = if (useSsl) "https" else "http"
                if (port != null && port > 0) "$scheme://$host:$port/" else "$scheme://$host/"
            } ?: FALLBACK_MIRROR
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo resolver mirrors (${e.message}). Usando fallback.")
            FALLBACK_MIRROR
        }
    }

    /**
     * Busca emisoras en Radio Browser.
     *
     * @param query   Texto a buscar en el nombre de la emisora.
     * @param country País como nombre (ej. "España"/"Spain") o código ISO 3166-1
     *                alpha-2 (ej. "ES"). Se convierte a código antes de llamar a
     *                la API, que filtra por `countrycode`.
     * @param genre   Género/etiqueta opcional.
     */
    suspend fun searchStations(query: String, country: String, genre: String?): List<Station> {
        val stations = api().searchStations(
            name = query.ifBlank { null },
            countryCode = toCountryCode(country),
            tag = genre?.ifBlank { null }
        )
        return stations.mapNotNull { it.toDomainStation() }
    }

    /**
     * Convierte un nombre de país (español/inglés) a su código ISO 3166-1 alpha-2.
     * La API de Radio Browser filtra por código (`countrycode`): `ES`, `DE`, etc.
     * Devuelve `null` cuando el país está vacío o no se reconoce (sin filtro).
     */
    private fun toCountryCode(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = normalizeCountryText(raw)
        if (normalized.length == 2) return normalized.uppercase()
        return COUNTRY_TO_CODE[normalized]
    }

    /** Minúsculas y sin tildes, para buscar en el mapa de países. */
    private fun normalizeCountryText(text: String): String =
        text.trim().lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ü", "u")
            .replace("ñ", "n")

    private companion object {
        /** Nombres de país normalizados (es/en/de/fr/it/ja) → código ISO 3166-1 alpha-2. */
        val COUNTRY_TO_CODE = mapOf(
            "espana" to "ES", "spain" to "ES", "spanien" to "ES", "espagne" to "ES",
            "spagna" to "ES", "スペイン" to "ES",
            "alemania" to "DE", "germany" to "DE", "deutschland" to "DE",
            "allemagne" to "DE", "germania" to "DE", "ドイツ" to "DE",
            "francia" to "FR", "france" to "FR", "frankreich" to "FR", "フランス" to "FR",
            "italia" to "IT", "italy" to "IT", "italien" to "IT", "italie" to "IT", "イタリア" to "IT",
            "portugal" to "PT",
            "reino unido" to "GB", "uk" to "GB", "great britain" to "GB",
            "irlanda" to "IE", "ireland" to "IE",
            "paises bajos" to "NL", "holanda" to "NL", "netherlands" to "NL",
            "belgica" to "BE", "belgium" to "BE",
            "suiza" to "CH", "switzerland" to "CH",
            "austria" to "AT",
            "grecia" to "GR", "greece" to "GR",
            "polonia" to "PL", "poland" to "PL",
            "suecia" to "SE", "sweden" to "SE",
            "noruega" to "NO", "norway" to "NO",
            "dinamarca" to "DK", "denmark" to "DK",
            "finlandia" to "FI", "finland" to "FI",
            "estados unidos" to "US", "ee. uu." to "US", "ee.uu." to "US",
            "usa" to "US", "united states" to "US", "vereinigte staaten" to "US",
            "etats unis" to "US", "stati uniti" to "US", "アメリカ" to "US",
            "mexico" to "MX",
            "argentina" to "AR",
            "colombia" to "CO",
            "chile" to "CL",
            "peru" to "PE",
            "venezuela" to "VE",
            "uruguay" to "UY",
            "paraguay" to "PY",
            "bolivia" to "BO",
            "ecuador" to "EC",
            "cuba" to "CU",
            "republica dominicana" to "DO",
            "brasil" to "BR", "brazil" to "BR",
            "japon" to "JP", "japan" to "JP", "giappone" to "JP", "日本" to "JP"
        )
    }

    /** Convierte un DTO de la API en una emisora de dominio (descarta entradas rotas). */
    private fun StationDto.toDomainStation(): Station? {
        val id = stationuuid ?: return null
        val nombre = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val streamUrl = urlResolved?.trim()?.takeIf { it.isNotEmpty() }
            ?: url?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        return Station(
            id = id,
            name = nombre,
            url = streamUrl,
            country = country?.trim()?.takeIf { it.isNotEmpty() },
            genre = tags
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.joinToString(" / ")
                ?.takeIf { it.isNotEmpty() },
            isCustom = false
        )
    }
}
