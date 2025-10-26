// Spring Boot entry point. It boots the application and auto-configures
// components (controllers, services, JPA, etc.)
package org.example.app
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream
import com.google.auth.oauth2.GoogleCredentials

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class App

val serviceAccount = FileInputStream("src/main/resources/bikey-f17f3-firebase-adminsdk-fbsvc-006bc4c19d.json")

val options = FirebaseOptions.builder()
    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    .build()

fun main(args: Array<String>) {
    FirebaseApp.initializeApp(options)
    runApplication<App>(*args)
}

