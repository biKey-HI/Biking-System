package org.example.app.pricingandpayment

import org.example.app.billing.TripSummaryDTO
import org.example.app.pricingandpayment.PaymentGateway
import org.example.app.user.User
import org.example.app.user.UserRole
import java.util.UUID

class PaymentContext(private var strategy: PricingStrategy) {

    fun setStrategy(newStrategy: PricingStrategy) {
        strategy = newStrategy
    }

    fun executePayment(user: User, summary: TripSummaryDTO, gateway: PaymentGateway): PaymentResult {
        return if(user.role == UserRole.RIDER)
            strategy.processPayment(user, summary, gateway)
        else
            PaymentResult(true, "No payments needed for operators.")
    }

    // Only for subscription payments
    fun executePayment(user: User, isYearly: Boolean, gateway: PaymentGateway): PaymentResult {
        if(user.role == UserRole.RIDER) {
            val payment = user.payment
                ?: return PaymentResult(false, "No payment method on file. Please add a card.")

            val token = payment.token ?: return PaymentResult(false, "Missing payment token.")
            val amount = if (isYearly) 11999 else 1499

            val result = gateway.charge(
                user.id!!,
                token,
                amount,
                idempotencyKey = "${if (isYearly) "yearly" else "monthly"} subscription: ${UUID.randomUUID()}"
            )
            return if (result.success)
                PaymentResult(
                    true,
                    "${if (isYearly) "Yearly" else "Monthly"} pass successfully purchased! Charged \$${amount / 100.0} to your card •••• ${payment.cardLast4}."
                )
            else
                PaymentResult(false, "Payment failed: ${result.failureReason}")
        } else
            return PaymentResult(true, "No payments needed for operators.")
    }

    fun requiresImmediatePayment(): Boolean = strategy.requiresImmediatePayment()
}
