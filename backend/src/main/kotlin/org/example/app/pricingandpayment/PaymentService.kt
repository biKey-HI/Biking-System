package org.example.app.pricingandpayment

import org.example.app.billing.TripSummaryDTO
//import org.example.app.pricingandpayment.PricingStrategy
//import org.example.app.pricingandpayment.MonthlySubscriptionStrategy
//import org.example.app.pricingandpayment.DefaultPayNowStrategy
//import org.example.app.pricingandpayment.PaymentContext
import org.example.app.user.PaymentRepository
import org.example.app.user.PaymentStrategyType
import org.example.app.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PaymentService(
    private val gateway: PaymentGateway,
    private val payments: PaymentRepository,
) {

    // Choose correct strategy for this user
    private fun createContext(user: User): PaymentContext {
        val strategy = when (user.paymentStrategy) {
            PaymentStrategyType.DEFAULT_PAY_NOW -> DefaultPayNowStrategy()
            PaymentStrategyType.MONTHLY_SUBSCRIPTION -> MonthlySubscriptionStrategy()
        }
        return PaymentContext(strategy)
    }

    // Core call after trip return
    fun handlePayment(user: User, summary: TripSummaryDTO): PaymentResult {
        val context = createContext(user)
        return context.executePayment(user, summary, gateway)
    }

    // Used by UI to know if we must show payment form
    fun requiresImmediatePayment(user: User): Boolean {
        return createContext(user).requiresImmediatePayment()
    }

    // Allow user to switch plans
    fun setStrategy(user: User, type: PaymentStrategyType) {
        val newStrategy = when (type) {
            PaymentStrategyType.DEFAULT_PAY_NOW -> DefaultPayNowStrategy()
            PaymentStrategyType.MONTHLY_SUBSCRIPTION -> MonthlySubscriptionStrategy()
        }
        val context = createContext(user)
        context.setStrategy(newStrategy)
        user.paymentStrategy = type
    }

    // helpers used by return controller

    data class SavedCardView(val hasSavedCard: Boolean, val provider: String? = null, val last4: String? = null)

    fun getSavedCard(userId: java.util.UUID): SavedCardView {
        val p = payments.findByUserId(userId)
        return if (p != null && p.token != null && p.cardLast4 != null)
            SavedCardView(true, p.provider, p.cardLast4)
        else
            SavedCardView(false)
    }

    fun saveCard(user: User, cardNumber: String, expMonth: Int, expYear: Int, cvc: String): SavedCardView {
        val tok = gateway.tokenizeCard(cardNumber, expMonth, expYear, cvc)
        val existing = payments.findByUserId(user.id!!)
        val entity = if (existing != null) {
            existing.copy(
                provider = tok.provider,
                token = tok.token,
                cardBrand = tok.brand,
                cardLast4 = tok.last4,
                cardNumberEnc = null
            )
        } else {
            org.example.app.user.Payment(
                user = user,
                cardHolderName = "${user.firstName} ${user.lastName}",
                provider = tok.provider,
                token = tok.token,
                cardBrand = tok.brand,
                cardLast4 = tok.last4,
                cardNumberEnc = null
            )
        }
        payments.save(entity)
        return SavedCardView(true, tok.provider, tok.last4)
    }
}

