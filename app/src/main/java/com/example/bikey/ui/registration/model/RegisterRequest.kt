package com.example.bikey.ui.registration.model

import kotlinx.serialization.Serializable


enum class UserRole { RIDER, OPERATOR }
@Serializable
data class RegisterRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val password: String,
    val role: UserRole = UserRole.RIDER,
    val address: AddressPayload,
    val payment: PaymentPayload? = null
)


@Serializable
data class RegisterResponse(
    val id: Long,
    val email: String
)
@Serializable
enum class Province { AB, BC, MB, NB, NL, NS, NT, NU, ON, PE, QC, SK, YT }

@Serializable
data class AddressPayload(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val province: Province,
    val postalCode: String,
    val country: String = "CA"
)

@Serializable
data class PaymentPayload(
    val cardHolderName: String,
    val provider: String? = null,
    val token: String? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val cardNumber: String? = null,
    val cvv3: String? = null
)
