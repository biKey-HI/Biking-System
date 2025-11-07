package org.example.app.pricingandpayment

import org.example.app.billing.TripSummaryDTO
import org.example.app.pricingandpayment.PaymentGateway
import org.example.app.user.User

class DefaultPayNowStrategy : PricingStrategy {

    override fun requiresImmediatePayment(): Boolean = true

    override fun processPayment(user: User, summary: TripSummaryDTO, gateway: PaymentGateway): PaymentResult {
        val payment = user.payment
            ?: return PaymentResult(false, "No payment method on file. Please add a card.")

        val token = payment.token ?: return PaymentResult(false, "Missing payment token.")
        val amount = summary.cost.totalCents

        val result = gateway.charge(user.id!!, token, amount, idempotencyKey = "trip:${summary.tripId}")
        return if (result.success)
            PaymentResult(true, "Charged \$${amount / 100.0} to your card •••• ${payment.cardLast4}.")
        else
            PaymentResult(false, "Payment failed: ${result.failureReason}")
    }
}
