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
    val role: UserRole = UserRole.RIDER
)


@Serializable
data class RegisterResponse(
    val id: Long,
    val email: String
)
