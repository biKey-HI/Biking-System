package org.example.app.user

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
open class AuthController(
    private val auth: AuthService
) {
    @PostMapping("/register")
    fun register(@RequestBody @Valid body: RegisterRequest): ResponseEntity<RegisterResponse> {
        val res = auth.register(body)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @GetMapping("/health")
    fun health() = mapOf("status" to "ok")
}

