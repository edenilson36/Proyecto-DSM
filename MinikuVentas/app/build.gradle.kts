// RUTA: build.gradle.kts (Module :app)
// ¡¡ESTE ES EL CÓDIGO CORREGIDO Y COMPLETO PARA ESTE ARCHIVO!!

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("org.jetbrains.kotlin.kapt") // <-- ¡PLUGIN AÑADIDO!
}

android {
    namespace = "com.udb.minikuventas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.udb.minikuventas"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)

    // ¡¡LIBRERÍAS AÑADIDAS PARA EL NUEVO DISEÑO!!
    implementation(libs.androidx.recyclerview) // Para la lista nueva
    implementation(libs.androidx.cardview)   // Para las tarjetas
    implementation(libs.glide)               // Para cargar imágenes
    kapt(libs.glide.compiler)                // Para que Glide funcione

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}