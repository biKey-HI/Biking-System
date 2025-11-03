package org.example.app.pricingandpayment

import org.example.app.billing.TripSummaryDTO
import org.example.app.pricingandpayment.PaymentGateway
import org.example.app.user.User

class PaymentContext(private var strategy: PricingStrategy) {

    fun setStrategy(newStrategy: PricingStrategy) {
        strategy = newStrategy
    }

    fun executePayment(user: User, summary: TripSummaryDTO, gateway: PaymentGateway): PaymentResult {
        return strategy.processPayment(user, summary, gateway)
    }

    fun requiresImmediatePayment(): Boolean = strategy.requiresImmediatePayment()
}
