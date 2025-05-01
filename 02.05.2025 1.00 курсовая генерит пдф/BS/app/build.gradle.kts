plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.bs"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bs"
        minSdk = 24
        targetSdk = 35
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // CameraX - библиотека для работы с камерой в Android
    implementation("androidx.camera:camera-core:1.2.0")          // Базовые функции CameraX
    implementation("androidx.camera:camera-camera2:1.2.0")       // Поддержка Camera2 API
    implementation("androidx.camera:camera-lifecycle:1.2.0")     // Управление жизненным циклом камеры
    implementation("androidx.camera:camera-view:1.2.0")          // Виджет для отображения preview камеры

    // Kotlin Coroutines - для асинхронных операций
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")  // Асинхронное программирование

    // iText - работа с PDF-документами
    implementation("com.itextpdf:itext7-core:7.1.14")            // Создание и редактирование PDF
    implementation("com.itextpdf:itextpdf:5.5.13.3")

    // Android Lifecycle компоненты
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")  // ViewModel + Kotlin расширения
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")   // LiveData + Kotlin расширения
}