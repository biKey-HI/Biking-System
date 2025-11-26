plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

springBoot {
    mainClass.set("org.example.app.AppKt")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))

    // Web + JSON + Validation
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // JPA + SQLite
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-community-dialects:6.5.2.Final")
    runtimeOnly("org.xerial:sqlite-jdbc:3.45.3.0")

    // BCrypt
    implementation("org.springframework.security:spring-security-crypto:6.3.2")

    implementation("org.apache.commons:commons-compress:1.26.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Timers
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Firebase
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.7")
}

tasks.test { useJUnitPlatform() }
