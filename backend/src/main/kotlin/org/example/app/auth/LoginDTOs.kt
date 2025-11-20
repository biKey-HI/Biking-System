package org.example.app.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.example.app.pricingandpayment.PricingStrategy
import org.example.app.user.PaymentStrategyType
import java.util.UUID

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8, max = 72) val password: String,
    val notificationToken: String
)

data class LoginResponse(
    val token: String,

    val email: String,

    val userId: UUID,

    val role: String,

    val pricingPlan: PaymentStrategyType? = null,

    val loyaltyTier: String? = null,

    val loyaltyTierDisplayName: String? = null,

    val tierChanged: Boolean = false,

    val tierUpgraded: Boolean = false,

    val tierDowngraded: Boolean = false,

    val oldTier: String? = null,

    val newTier: String? = null

    val pricingPlan: PaymentStrategyType? = null,
    val flexDollars: Float = 0.0f
)
