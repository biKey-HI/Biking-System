// It's the REST controller mounted at /api/auth. Exposes POST /api/auth/register
// and expects a JSON body (annotated with @RequestBody) and runs bean validation (@Valid).
// Returns 201 Created on success with a small response payload.
package org.example.app.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)

    fun register(@Valid @RequestBody req: RegisterRequest): RegisterResponse =
        authService.register(req)

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): LoginResponse =
        authService.login(req)

    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody req: ForgotPasswordRequest): ResponseEntity<ForgotPasswordResponse> {
        val response = authService.sendPasswordResetLink(req)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody req: ResetPasswordRequest): ResponseEntity<ResetPasswordResponse> {
        val response = authService.resetPassword(req)
        return ResponseEntity.ok(response)
    }
}
