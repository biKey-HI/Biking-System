pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Android + Kotlin
        id("com.android.application") version "8.13.0"
        kotlin("android") version "2.0.0"
        kotlin("jvm") version "2.0.0"
        kotlin("plugin.spring") version "2.0.0"
        kotlin("plugin.jpa") version "2.0.0"

        // Spring Boot
        id("org.springframework.boot") version "3.3.3"
        id("io.spring.dependency-management") version "1.1.6"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "biKey"
include(":app", ":backend")
