plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
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

    //to send an email
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // JPA + SQLite
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-community-dialects:6.5.2.Final")
    runtimeOnly("org.xerial:sqlite-jdbc:3.45.3.0")

    // BCrypt
    implementation("org.springframework.security:spring-security-crypto:6.3.2")

    implementation("org.apache.commons:commons-compress:1.26.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test { useJUnitPlatform() }
