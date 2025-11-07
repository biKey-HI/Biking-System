package org.example.app.pricingandpayment

import java.util.UUID

interface PaymentGateway {
    data class TokenizedCard(
        val provider: String,
        val token: String,
        val brand: String,
        val last4: String
    )
    data class ChargeResult(
        val success: Boolean,
        val providerChargeId: String?,
        val failureReason: String? = null
    )
    fun tokenizeCard(cardNumber: String, expMonth: Int, expYear: Int, cvc: String): TokenizedCard
    fun charge(userId: UUID, token: String, amountCents: Int, currency: String = "CAD", idempotencyKey: String): ChargeResult
}
