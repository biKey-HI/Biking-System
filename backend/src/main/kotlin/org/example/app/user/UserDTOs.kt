//package org.example.app.user
//
//import jakarta.validation.constraints.Email
//import jakarta.validation.constraints.NotBlank
//import jakarta.validation.constraints.Size
//
//data class RegisterRequest(
//    @field:Email(message = "Invalid email")
//    @field:NotBlank
//    val email: String,
//
//    @field:Size(min = 8, message = "Password must be at least 8 characters")
//    val password: String,
//
//    @field:NotBlank
//    @field:Size(max = 120)
//    val displayName: String
//)
//
//data class RegisterResponse(
//    val id: Long,
//    val email: String,
//    val displayName: String
//)
