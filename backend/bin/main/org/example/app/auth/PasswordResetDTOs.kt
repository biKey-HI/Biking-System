package org.example.app.auth

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)

data class ForgotPasswordResponse(
    val message: String = "If the email exists, a password reset link has been sent."
)

data class ResetPasswordResponse(
    val message: String = "Password has been reset successfully."
)