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
    val isRider: Boolean,
    val isOperator: Boolean,
    val pricingPlan: PricingPlan,

    val loyaltyTier: String? = null,

    val loyaltyTierDisplayName: String? = null,

    val tierChanged: Boolean = false,

    val tierUpgraded: Boolean = false,

    val tierDowngraded: Boolean = false,

    val oldTier: String? = null,

    val newTier: String? = null,

    val flexDollars: Float,

    val kilometersTravelled: Int = 0
)