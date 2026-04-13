import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

// Читаем токены из local.properties (безопасность) или environment variables (CI/CD)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Функция для получения значения из local.properties или environment
fun getConfigValue(key: String, default: String = ""): String {
    return localProperties.getProperty(key) 
        ?: System.getenv(key) 
        ?: default
}

android {
    namespace = "com.autoclicker.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.autoclicker.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 117
        versionName = "1.1.7"
        
        // Crash reporting credentials (читаем из local.properties или environment)
        buildConfigField("String", "CRASH_BOT_TOKEN", "\"${getConfigValue("CRASH_BOT_TOKEN")}\"")
        buildConfigField("String", "CRASH_CHAT_ID", "\"${getConfigValue("CRASH_CHAT_ID")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Отключаем минификацию для отладки
            isMinifyEnabled = false
            // Добавляем суффикс для отладочной версии
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.9.3")
    
    // Coroutines для асинхронных операций
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    
    // ML Kit для OCR
    implementation("com.google.mlkit:text-recognition:16.0.1")
    
    // Telegram Bot API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON
    implementation("com.google.code.gson:gson:2.11.0")
    
    // QR Code генерация
    implementation("com.google.zxing:core:3.5.3")
    
    // QR Code сканирование
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    
    // EncryptedSharedPreferences для безопасного хранения токенов
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Hilt для Dependency Injection
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")
    
    // Unit тесты
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.4.4")
    
    // Instrumented тесты
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
