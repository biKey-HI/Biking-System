package org.example.app.bmscoreandstationcontrol.persistence

import org.example.app.bmscoreandstationcontrol.persistence.TripStatus
import org.example.app.bmscoreandstationcontrol.persistence.Trip
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class TripTest {

    @Test
    fun `trip initializes with IN_PROGRESS`() {
        val t = Trip(
            riderId = UUID.randomUUID(),
            bikeId = UUID.randomUUID(),
            startStationId = UUID.randomUUID()
        )

        assertEquals(TripStatus.IN_PROGRESS, t.status)
        assertNotNull(t.startedAt)
    }

    @Test
    fun `trip allows nullable dest and end time`() {
        val t = Trip(
            riderId = UUID.randomUUID(),
            bikeId = UUID.randomUUID(),
            startStationId = UUID.randomUUID()
        )
        assertNull(t.destStationId)
        assertNull(t.endedAt)
    }
}
