package org.example.app.bmscoreandstationcontrol.persistence

import org.example.app.bmscoreandstationcontrol.domain.Dock
import org.example.app.bmscoreandstationcontrol.domain.DockState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class DockEntityTest {

    @Test
    fun `default dock entity has empty state`() {
        val d = DockEntity()
        assertEquals(DockState.EMPTY, d.status)
    }

    @Test
    fun `dock entity can convert domain dock`() {
        val domain = Dock(id = UUID.randomUUID(), bike = null, status = DockState.OCCUPIED)
        val entity = DockEntity(domain)

        assertEquals(domain.id, entity.id)
        assertEquals(DockState.OCCUPIED, entity.status)
    }

    @Test
    fun `dock entity converts back to domain`() {
        val de = DockEntity()
        val domain = de.toDomain()
        assertEquals(de.id, domain.id)
        assertEquals(de.status, domain.status)
    }
}

class DockStateConverterTest {
    private val converter = DockStateConverter()

    @Test
    fun `converter stores dockstate as displayName`() {
        val result = converter.convertToDatabaseColumn(DockState.EMPTY)
        assertEquals("Empty", result)
    }

    @Test
    fun `converter reconstructs dockstate`() {
        val state = converter.convertToEntityAttribute("Occupied")
        assertEquals(DockState.OCCUPIED, state)
    }
}
