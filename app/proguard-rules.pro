# Autor: jjcampac - IA: cline
#
# Reglas de R8 propias de la app Eter.
#
# Se aplican SOLO a la variante release (ver build.gradle.kts ->
# buildTypes.release -> optimization -> keepRules -> files).
# No se desactiva la minificación ni la ofuscación: únicamente se conserva lo
# que las librerías leen por reflexión en tiempo de ejecución y que R8 no puede
# detectar por análisis estático.

# ---------------------------------------------------------------------------
# Modelo de Radio Browser (Retrofit + Gson)
# ---------------------------------------------------------------------------
# Error real detectado en release (logcat, tag RadioBrowserRepo):
#   java.lang.ClassCastException lanzada en Retrofit al invocar la función
#   `suspend` (HttpServiceMethod.parseAnnotations hace
#   `(ParameterizedType) parameterTypes[last]` sobre el Continuation).
#
# Motivo: StationDto y ServerDto solo aparecen en posiciones genéricas de la
# interfaz de Retrofit (`Continuation<? super List<StationDto>>`) y en la
# reflexión de Gson. R8 no ve esas referencias, elimina las clases y reescribe
# la firma genérica; al no quedar el argumento de tipo, Retrofit recibe un
# Continuation sin genérico y lanza ClassCastException.
#
# Esto es lo mismo que genera el "response type keeper" oficial de Retrofit
# (com.squareup.retrofit2:response-type-keeper, ver CHANGELOG 2.10.0), pero
# escrito a mano para no añadir una dependencia nueva.
#
# Además se conservan los campos porque Gson los rellena por reflexión; es la
# configuración oficial de Gson para el modelo:
#   -keep class <modelo>.** { <fields>; }
-keep class com.jjcampac.eter.data.remote.dto.StationDto { <fields>; }
-keep class com.jjcampac.eter.data.remote.dto.ServerDto { <fields>; }

# ---------------------------------------------------------------------------
# Qué NO se añade aquí (y por qué), para no meter reglas a ciegas
# ---------------------------------------------------------------------------
# - `-keepattributes Signature` y `-keepattributes *Annotation*`:
#   ya los aplica el fichero por defecto de AGP
#   (proguard-android-optimize.txt) y, además, retrofit2.pro. Se comprueba en
#   app/build/outputs/mapping/release/configuration.txt.
# - `-keep class kotlin.coroutines.Continuation`:
#   ya viene en retrofit2.pro de Retrofit 2.11.0
#   ("-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation").
# - `-keep ... interface * { @retrofit2.http.* <methods>; }`:
#   también lo aporta retrofit2.pro (mantiene las interfaces de servicio).
# - Reglas genéricas de Gson (TypeToken, @JsonAdapter, JsonSerializer...):
#   la app no usa TypeToken ni adaptadores propios; el convertidor de Retrofit
#   construye el Type a partir de los genéricos ya conservados.
# - Reglas de OkHttp/Okio: sus propios ficheros (okhttp3.pro) ya se aplican.
