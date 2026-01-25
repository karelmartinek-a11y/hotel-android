// Top-level build.gradle.kts for HOTEL Android
// Stack: Kotlin + Jetpack Compose, MVVM, Retrofit/OkHttp, kotlinx.serialization, Room, WorkManager.
// No Firebase/FCM/Play Integrity.

plugins {
    // Using version catalog in settings.gradle.kts would be ideal, but keeping deterministic here.
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
