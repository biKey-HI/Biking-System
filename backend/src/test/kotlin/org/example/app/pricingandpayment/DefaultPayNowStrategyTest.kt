package org.example.app.pricingandpayment

import io.mockk.*
import org.example.app.billing.CostBreakdownDTO
import org.example.app.billing.TripSummaryDTO
import org.example.app.user.Payment
import org.example.app.user.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class DefaultPayNowStrategyTest {

    private fun dummySummary() =
        TripSummaryDTO(
            tripId = UUID.randomUUID(),
            riderId = UUID.randomUUID(),
            bikeId = UUID.randomUUID(),
            startStationName = "A",
            endStationName = "B",
            startTime = Instant.now(),
            endTime = Instant.now(),
            durationMinutes = 5,
            isEBike = false,
            cost = CostBreakdownDTO(
                baseCents = 0,
                perMinuteCents = 0,
                minutes = 0,
                eBikeSurchargeCents = 0,
                overtimeCents = 0,
                flexDollarCents = 0,
                totalCents = 500
            )
        )

    @Test
    fun `fails when user has no payment object`() {
        val user = mockk<User>()
        val gateway = mockk<PaymentGateway>()

        every { user.payment } returns null

        val result = DefaultPayNowStrategy().processPayment(user, dummySummary(), gateway)

        assertFalse(result.success)
        assertTrue(result.message.contains("No payment method", ignoreCase = true))
    }

    @Test
    fun `invokes payment gateway charge`() {
        val user = mockk<User>()
        val payment = mockk<Payment>()
        val gateway = mockk<PaymentGateway>()

        every { user.id } returns UUID.randomUUID()
        every { user.payment } returns payment

        every { payment.token } returns "tok_abc"
        every { payment.cardLast4 } returns "1234"

        every {
            gateway.charge(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns PaymentGateway.ChargeResult(
            success = true,
            providerChargeId = "ch_123"
        )

        val result = DefaultPayNowStrategy().processPayment(user, dummySummary(), gateway)

        assertTrue(result.success)

        verify(exactly = 1) {
            gateway.charge(
                any(),
                "tok_abc",
                500,
                any(),
                any()
            )
        }
    }
}
