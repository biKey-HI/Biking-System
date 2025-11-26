package org.example.app.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class UserTest {

    private fun dummyAddress() = Address(
        line1 = "123 St",
        city = "Montreal",
        province = Province.QC,
        postalCode = "H1H1H1"
    )

    @Test
    fun `useFlexDollars does not exceed available amount`() {
        val user = User(
            address = dummyAddress(),
            email = "test@test.com",
            passwordHash = "hash",
            firstName = "A",
            lastName = "B",
            username = "user1",
            flexDollars = 10f
        )

        val used = user.useFlexDollars(15f)

        assertEquals(10f, used)
        assertEquals(0f, user.flexDollars)
    }

    @Test
    fun `useFlexDollars subtracts correctly`() {
        val user = User(
            address = dummyAddress(),
            email = "test@test.com",
            passwordHash = "hash",
            firstName = "A",
            lastName = "B",
            username = "user2",
            flexDollars = 20f
        )

        val used = user.useFlexDollars(5f)

        assertEquals(5f, used)
        assertEquals(15f, user.flexDollars)
    }
}
