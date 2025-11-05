package org.example.app.pricingandpayment

import org.example.app.billing.TripSummaryDTO
import org.example.app.pricingandpayment.PaymentGateway
import org.example.app.user.User

interface PricingStrategy {
    /** if the strategy requires the user to pay immediately after trip */
    fun requiresImmediatePayment(): Boolean

    /** payment logic (charge, etc.) */
    fun processPayment(user: User, summary: TripSummaryDTO, gateway: PaymentGateway): PaymentResult
}

data class PaymentResult(
    val success: Boolean,
    val message: String
)
