package org.example.app.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class PaymentRepositoryTest {

    private val repo = mockk<PaymentRepository>(relaxed = true)

    @Test
    fun `existsByUserId delegates to repository`() {
        val id = UUID.randomUUID()
        every { repo.existsByUserId(id) } returns true

        val result = repo.existsByUserId(id)

        assertTrue(result)
        verify { repo.existsByUserId(id) }
    }

    @Test
    fun `findByUserId returns payment`() {
        val id = UUID.randomUUID()
        val payment = mockk<Payment>()
        every { repo.findByUserId(id) } returns payment

        val result = repo.findByUserId(id)

        assertEquals(payment, result)
    }
}
