// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false

    // Kotlin 2.0 + Compose compiler plugin
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false

    // Kotlinx Serialization (you use @Serializable in the app)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false

    // Spring (backend)
    id("org.springframework.boot") version "3.5.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false

    // Firebase
    id("com.google.gms.google-services") version "4.4.4" apply false
}