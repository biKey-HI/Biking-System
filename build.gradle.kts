// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.20" apply false

    // Kotlin 2.0 + Compose compiler plugin
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false

    // Kotlinx Serialization (you use @Serializable in the app)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false

    // Spring (backend)
    id("org.springframework.boot") version "3.3.3" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false

}