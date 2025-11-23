package org.example.app.bmscoreandstationcontrol.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BikeStateTest {

    @Test
    fun `toString returns display names`() {
        assertEquals("Available", BikeState.AVAILABLE.toString())
        assertEquals("Reserved", BikeState.RESERVED.toString())
        assertEquals("On Trip", BikeState.ON_TRIP.toString())
        assertEquals("Maintenance", BikeState.MAINTENANCE.toString())
    }

    @Test
    fun `fromString parses correctly`() {
        assertEquals(BikeState.AVAILABLE, BikeState.fromString("Available"))
        assertEquals(BikeState.RESERVED, BikeState.fromString("Reserved"))
    }

    @Test
    fun `fromString throws on invalid`() {
        assertThrows(IllegalArgumentException::class.java) {
            BikeState.fromString("NotAState")
        }
    }
}
