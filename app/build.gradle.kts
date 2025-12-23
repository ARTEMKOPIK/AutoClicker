import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        versionCode = 1
        versionName = "1.0"
        
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    
    // ML Kit для OCR
    implementation("com.google.mlkit:text-recognition:16.0.3")
    
    // Telegram Bot API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON
    implementation("com.google.code.gson:gson:2.11.0")
    
    // QR Code генерация
    implementation("com.google.zxing:core:3.5.3")
    
    // QR Code сканирование
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}
