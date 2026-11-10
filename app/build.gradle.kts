/*
 * Autor: jjcampac
 * IA: cline
 */
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/** Versión base de la app (sin sufijo de build). El sufijo "d"/"r" se añade
 *  en cada build type para distinguir debug de release. */
val baseVersionName = "1.0.0"

/** Credenciales de la keystore de release. El fichero no se versiona (contiene
 *  contraseñas). Si no existe, la variante release se firma con la keystore de
 *  debug para poder instalar desde Android Studio en el móvil de pruebas. */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.jjcampac.eter"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jjcampac.eter"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = baseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Solo se crea si hay credenciales locales (keystore.properties).
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "d"
        }
        release {
            versionNameSuffix = "r"
            // Con keystore propia se usa esa firma; si no hay fichero de
            // credenciales, se firma con la de debug para poder desplegar
            // desde Android Studio en el móvil de pruebas.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            optimization {
                enable = true
                // Reglas propias de R8 (imprescindibles): ver app/proguard-rules.pro.
                // Solo se conservan las clases del modelo de Radio Browser, que
                // Retrofit y Gson leen por reflexión.
                keepRules {
                    files.add(file("proguard-rules.pro"))
                }
                // OJO: no usar `packageScope`. Con AGP 9.5 activa el modo "gradual R8"
                // (android.r8.gradual.support, experimental) y genera un dex inválido:
                // deja clases package-private accesibles desde otro paquete, lo que
                // provoca IllegalAccessError al abrir la base de datos Room
                // (kotlin.text.i extendida por wf2). Sin packageScope se usa el
                // pipeline estándar de R8 (equivale a "**").
            }
        }
    }

    /**
     * No generar los ficheros de metadatos de dex (.dm) junto al APK.
     *
     * Android Studio, al desplegar, comprueba justo después de instalar que el perfil
     * de arranque (baseline profile) haya quedado aplicado: ejecuta `pm art dump` y
     * exige ver "status=speed-profile". Si el .dm no se envía en esa instalación (o
     * el sistema compila con el filtro "verify"), el despliegue falla con
     * INSTALL_BASELINE_PROFILE_FAILED aunque la app se haya instalado bien.
     *
     * El perfil sigue dentro del APK (assets/dexopt/baseline.prof) y
     * androidx.profileinstaller lo instala en el primer arranque de la app.
     */
    installation {
        enableBaselineProfile = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Núcleo y Compose
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Reproducción: Media3 (ExoPlayer + HLS + MediaSession + UI)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Persistencia: Room (Fase 3)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Corrutinas
    implementation(libs.kotlinx.coroutines.android)

    // Red (Retrofit/OkHttp) — Fase 4: API pública de Radio Browser
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)

    // Carga de imágenes (Coil 3 + red OkHttp) — logos de emisoras
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // Listas reordenables (drag & drop en Favoritas)
    implementation(libs.reorderable)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
