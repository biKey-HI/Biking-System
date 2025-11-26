package org.example.app.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AddressTest {

    @Test
    fun `default country is CA`() {
        val a = Address(
            line1 = "123",
            city = "Montreal",
            province = Province.QC,
            postalCode = "H1H1H1"
        )
        assertEquals("CA", a.country)
    }

    @Test
    fun `users and docking stations lists are empty by default`() {
        val a = Address(
            line1 = "123",
            city = "Montreal",
            province = Province.QC,
            postalCode = "H1H1H1"
        )

        assertTrue(a.users.isEmpty())
        assertTrue(a.dockingStations.isEmpty())
    }

    @Test
    fun `two addresses with same fields are equal`() {
        val a1 = Address(
            line1 = "123",
            city = "Montreal",
            province = Province.QC,
            postalCode = "H1H1H1"
        )

        val a2 = Address(
            line1 = "123",
            city = "Montreal",
            province = Province.QC,
            postalCode = "H1H1H1"
        )

        assertEquals(a1.copy(id = null), a2.copy(id = null))
    }
}
