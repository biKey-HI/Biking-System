package org.example.app.pricingandpayment

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PricingStrategyComponentsTest {

    @Test
    fun `payment result holds provided values`() {
        val r = PaymentResult(true, "ok")
        assertTrue(r.success)
        assertEquals("ok", r.message)
    }
}
