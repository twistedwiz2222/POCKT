plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.pockt.app"
    compileSdk = 36

    val pocktStoreFile = System.getenv("POCKT_KEYSTORE_PATH")
    val pocktStorePassword = System.getenv("POCKT_KEYSTORE_PASSWORD")
    val pocktKeyAlias = System.getenv("POCKT_KEY_ALIAS")
    val pocktKeyPassword = System.getenv("POCKT_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        pocktStoreFile,
        pocktStorePassword,
        pocktKeyAlias,
        pocktKeyPassword
    ).all { !it.isNullOrBlank() }

    defaultConfig {
        applicationId = "com.pockt.money"
        minSdk = 23
        targetSdk = 36
        versionCode = 8
        versionName = "0.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("safe") {
            dimension = "distribution"
            applicationIdSuffix = ".safe"
            versionNameSuffix = "-safe"
            buildConfigField("boolean", "PAYMENT_DETECTOR_ENABLED", "false")
            manifestPlaceholders["notificationListenerEnabled"] = "false"
        }
        create("full") {
            dimension = "distribution"
            versionNameSuffix = "-full"
            buildConfigField("boolean", "PAYMENT_DETECTOR_ENABLED", "true")
            manifestPlaceholders["notificationListenerEnabled"] = "true"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(pocktStoreFile!!)
                storePassword = pocktStorePassword
                keyAlias = pocktKeyAlias
                keyPassword = pocktKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

kapt { correctErrorTypes = true }
