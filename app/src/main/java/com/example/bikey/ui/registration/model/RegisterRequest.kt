package com.example.bikey.ui.registration.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val password: String
)


@Serializable
data class RegisterResponse(
    val id: Long,
    val email: String
)
