package com.example.bikey.ui.forgotpassword.model

import kotlinx.serialization.Serializable

@Serializable
data class ForgotPasswordRequest(
    val email: String
)
