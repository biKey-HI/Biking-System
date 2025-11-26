package org.example.app.bmscoreandstationcontrol.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DockStateTest {

    @Test
    fun `toString returns display names`() {
        assertEquals("Empty", DockState.EMPTY.toString())
        assertEquals("Occupied", DockState.OCCUPIED.toString())
        assertEquals("Out of Service", DockState.OUT_OF_SERVICE.toString())
    }

    @Test
    fun `fromString parses correctly`() {
        assertEquals(DockState.EMPTY, DockState.fromString("Empty"))
    }

    @Test
    fun `fromString throws on invalid`() {
        assertThrows(IllegalArgumentException::class.java) {
            DockState.fromString("Nope")
        }
    }
}
