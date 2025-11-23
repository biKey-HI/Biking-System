package org.example.app.bmscoreandstationcontrol.persistence

import org.example.app.bmscoreandstationcontrol.domain.*
import org.example.app.user.Address
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class DockingStationEntityTest {

    private fun sampleAddress() = Address(
        line1 = "123",
        city = "Montreal",
        province = org.example.app.user.Province.QC,
        postalCode = "H1H1H1"
    )

    @Test
    fun `entity can be instantiated`() {
        val e = DockingStationEntity(
            name = "Station A",
            address = sampleAddress(),
            location = LatLng(45.0, -73.0)
        )
        assertEquals("Station A", e.name)
        assertEquals(20, e.capacity)
        assertEquals(20, e.numFreeDocks)
    }

    @Test
    fun `entity converts from domain`() {
        val ds = DockingStation(
            name = "Test",
            address = sampleAddress(),
            location = LatLng(10.0, 20.0),
            stateChanges = mutableListOf(),
            docks = mutableListOf(),
            reservationHoldTime = Duration.ofMinutes(10)
        )
        val e = DockingStationEntity(ds)

        assertEquals(ds.id, e.id)
        assertEquals(ds.name, e.name)
        assertEquals(ds.location.latitude, e.location.latitude)
    }

    @Test
    fun `entity converts back to domain`() {
        val e = DockingStationEntity(
            name = "Station B",
            address = sampleAddress(),
            location = LatLng(),
            status = "Empty"
        )
        val domain = e.toDomain()

        assertEquals(e.id, domain.id)
        assertEquals("Station B", domain.name)
        assertEquals(Empty(domain).toString(), domain.status.toString())
    }

    @Test
    fun `toResponse returns correct wrapper`() {
        val e = DockingStationEntity(
            name = "X",
            address = sampleAddress(),
            location = LatLng(0.0, 0.0)
        )
        val r = e.toResponse()

        assertEquals("X", r.name)
        assertEquals("Montreal", r.address.city)
    }
}
