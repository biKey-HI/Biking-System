package org.example.app.pricingandpayment

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StubPaymentGateway : PaymentGateway {
    override fun tokenizeCard(cardNumber: String, expMonth: Int, expYear: Int, cvc: String) =
        PaymentGateway.TokenizedCard("stub", "tok_${UUID.randomUUID()}", "VISA", cardNumber.takeLast(4))

    override fun charge(userId: UUID, token: String, amountCents: Int, currency: String, idempotencyKey: String) =
        PaymentGateway.ChargeResult(true, "ch_${UUID.randomUUID()}", null)
}
