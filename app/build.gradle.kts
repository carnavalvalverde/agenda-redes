import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Datos de firma fuera del repo: ruta en la variable de entorno AGENDA_KEYSTORE_PROPS
// o en ~/agenda-redes-firma/keystore.properties
val firmaProps = Properties().apply {
    val ruta = System.getenv("AGENDA_KEYSTORE_PROPS")
        ?: "${System.getProperty("user.home")}/agenda-redes-firma/keystore.properties"
    val f = file(ruta)
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "es.carnavalverde.agenda"
    compileSdk = 35

    defaultConfig {
        applicationId = "es.carnavalverde.agenda"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (firmaProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(firmaProps.getProperty("storeFile"))
                storePassword = firmaProps.getProperty("storePassword")
                keyAlias = firmaProps.getProperty("keyAlias")
                keyPassword = firmaProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-video:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")

    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
}
