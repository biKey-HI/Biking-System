package com.example.bikey.ui.login.model

import com.example.bikey.ui.PricingPlan
import kotlinx.serialization.Serializable
import java.util.UUID
import com.example.bikey.ui.UUIDSerializer

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val notificationToken: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val email: String,
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID,
    val role: String,
    val pricingPlan: PricingPlan,
    val flexDollars: Float
)