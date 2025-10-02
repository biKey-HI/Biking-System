// DTOs (Data Transfer Objects) + validation rules: RegisterRequest requires email, password,
// and displayName (all non-blank; email must be valid; password min 8 chars).
// RegisterResponse returns id and email.
package org.example.app.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8, max = 72) val password: String,
    @field:NotBlank @field:Size(max = 120) val displayName: String
)

data class RegisterResponse(
    val id: Long,
    val email: String
)
