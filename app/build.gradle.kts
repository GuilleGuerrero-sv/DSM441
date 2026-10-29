import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Las claves se leen de este archivo local; no las escribimos en el codigo.
val clavesLocales = Properties().apply {
    val archivo = rootProject.file("secrets.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
fun leerCredencial(nombre: String): String {
    val valor = (System.getenv(nombre) ?: clavesLocales.getProperty(nombre, "")).trim()
    return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "").replace("\r", "") + "\""
}

android {
    namespace = "sv.edu.atlasbrisa"
    compileSdk = 37

    defaultConfig {
        applicationId = "sv.edu.atlasbrisa"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "REST_COUNTRIES_TOKEN", leerCredencial("REST_COUNTRIES_TOKEN"))
        buildConfigField("String", "GEONAMES_USERNAME", leerCredencial("GEONAMES_USERNAME"))
        buildConfigField("String", "WEATHER_API_KEY", leerCredencial("WEATHER_API_KEY"))
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
