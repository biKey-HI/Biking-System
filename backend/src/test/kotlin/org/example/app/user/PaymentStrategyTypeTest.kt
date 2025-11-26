package org.example.app.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PaymentStrategyTypeTest {

    @Test
    fun `toString returns display names`() {
        assertEquals("Pay As You Go", PaymentStrategyType.DEFAULT_PAY_NOW.toString())
        assertEquals("Monthly Pass", PaymentStrategyType.MONTHLY_SUBSCRIPTION.toString())
        assertEquals("Annual Pass", PaymentStrategyType.ANNUAL_SUBSCRIPTION.toString())
    }

    @Test
    fun `fromString returns correct strategy`() {
        assertEquals(PaymentStrategyType.DEFAULT_PAY_NOW, PaymentStrategyType.fromString("Pay As You Go"))
        assertEquals(PaymentStrategyType.MONTHLY_SUBSCRIPTION, PaymentStrategyType.fromString("Monthly Pass"))
        assertEquals(PaymentStrategyType.ANNUAL_SUBSCRIPTION, PaymentStrategyType.fromString("Annual Pass"))
    }

    @Test
    fun `fromString returns null on invalid input`() {
        assertNull(PaymentStrategyType.fromString("invalid"))
    }
}
