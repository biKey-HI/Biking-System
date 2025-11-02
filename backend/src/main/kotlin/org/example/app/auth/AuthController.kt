// It's the REST controller mounted at /api/auth. Exposes POST /api/auth/register
// and expects a JSON body (annotated with @RequestBody) and runs bean validation (@Valid).
// Returns 201 Created on success with a small response payload.
package org.example.app.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

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
    fun login(@Valid @RequestBody req: LoginRequest): LoginResponse {
        return authService.login(req)
    }
}
