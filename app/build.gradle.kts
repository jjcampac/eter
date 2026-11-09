/*
 * Autor: jjcampac
 * IA: cline
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/** Versión base de la app (sin sufijo de build). El sufijo "d"/"r" se añade
 *  en cada build type para distinguir debug de release. */
val baseVersionName = "0.9.2"

android {
    namespace = "com.jjcampac.eter"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jjcampac.eter"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = baseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            versionNameSuffix = "d"
        }
        release {
            versionNameSuffix = "r"
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
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
