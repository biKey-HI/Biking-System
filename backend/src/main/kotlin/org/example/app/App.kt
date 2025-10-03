// Spring Boot entry point. It boots the application and auto-configures
// components (controllers, services, JPA, etc.)
package org.example.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}

