plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("kotlin-kapt")
}

android {
    namespace = "cz.hcasc.hotel"
    compileSdk = 34

    defaultConfig {
        applicationId = "cz.hcasc.hotel"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // App-side limits (also enforced server-side)
        buildConfigField("int", "MAX_PHOTOS_PER_REPORT", "5")
        buildConfigField("int", "MAX_DESCRIPTION_LEN", "50")

        // Polling interval hint (WorkManager minimum for periodic work is 15 minutes)
        buildConfigField("int", "POLL_INTERVAL_MINUTES", "15")

        // Networking
        buildConfigField("String", "BASE_URL", "\"https://hotel.hcasc.cz/\"")
        buildConfigField("String", "API_BASE_URL", "\"https://hotel.hcasc.cz/api/\"")
    }

    signingConfigs {
        // Real release signing is provided via gradle.properties or env vars.
        // Keep repo free of secrets; local ~/.gradle/gradle.properties is recommended.
        create("release") {
            val storePath = providers.gradleProperty("HOTEL_STORE_FILE")
                .orElse(providers.environmentVariable("HOTEL_STORE_FILE"))
                .orNull
            val storePass = providers.gradleProperty("HOTEL_STORE_PASSWORD")
                .orElse(providers.environmentVariable("HOTEL_STORE_PASSWORD"))
                .orNull
            val alias = providers.gradleProperty("HOTEL_KEY_ALIAS")
                .orElse(providers.environmentVariable("HOTEL_KEY_ALIAS"))
                .orNull
            val keyPass = providers.gradleProperty("HOTEL_KEY_PASSWORD")
                .orElse(providers.environmentVariable("HOTEL_KEY_PASSWORD"))
                .orNull

            if (storePath != null && storePass != null && alias != null && keyPass != null) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            } else {
                println("WARNING: Missing HOTEL_* signing props, release will be unsigned.")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            // Keep release builds simple on this host to avoid R8/desugar memory spikes.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation (Compose)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Material theme bridge for the Activity
    implementation("com.google.android.material:material:1.11.0")

    // Networking: OkHttp + Retrofit
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // kotlinx.serialization converter
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room (offline queue)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // CameraX (capture photos)
    val cameraX = "1.3.2"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    // Security (Keystore helpers, encrypted prefs if needed)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Images (thumbnails / photo preview)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Notifications
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
