package org.example.app.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8, max = 72) val password: String,
    val notificationToken: String
)

data class LoginResponse(
    val token: String,
    val email: String,
    val userId: UUID,
    val role: String
)
