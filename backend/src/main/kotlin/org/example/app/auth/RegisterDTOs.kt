// DTOs (Data Transfer Objects) + validation rules: RegisterRequest requires email, password,
// and displayName (all non-blank; email must be valid; password min 8 chars).
// RegisterResponse returns id and email.
package org.example.app.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.example.app.user.UserRole
import jakarta.validation.constraints.Size
import org.example.app.user.Province

data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8, max = 72) val password: String,
    @field:NotBlank @field:Size(max = 120) val firstName: String,
    @field:NotBlank @field:Size(max = 120) val lastName: String,
    @field:NotBlank @field:Size(max = 120) val username: String,
    val address: AddressPayload,
    val payment: PaymentPayload? = null,

    val role: UserRole = UserRole.RIDER
)

data class RegisterResponse(
    val id: Long,
    val email: String
)

// for page 2
data class AddressPayload(
    @field:NotBlank val line1: String,
    val line2: String? = null,
    @field:NotBlank val city: String,
    val province: Province,
    @field:NotBlank val postalCode: String,
    @field:NotBlank val country: String = "CA"
)

// for page 3 (skippable)
data class PaymentPayload(
    @field:NotBlank val cardHolderName: String,
    val provider: String? = null,
    val token: String? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val cardNumber: String? = null,
    val cvv3: String? = null
)