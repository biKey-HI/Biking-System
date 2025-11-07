package org.example.app.pricingandpayment

import org.example.app.billing.TripSummaryDTO
import org.example.app.pricingandpayment.PricingStrategy
import org.example.app.pricingandpayment.PaymentGateway
import org.example.app.pricingandpayment.PaymentResult
import org.example.app.user.User

class MonthlySubscriptionStrategy : PricingStrategy {
    override fun requiresImmediatePayment(): Boolean = false

    override fun processPayment(user: User, summary: TripSummaryDTO, gateway: PaymentGateway): PaymentResult {
        return PaymentResult(
            success = true,
            message = "Trip covered under monthly subscription."
        )
    }
}
