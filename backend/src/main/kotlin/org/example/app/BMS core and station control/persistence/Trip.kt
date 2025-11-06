package org.example.app.bmscoreandstationcontrol.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class TripStatus {
    IN_PROGRESS, COMPLETED
}

@Entity
@Table(name = "trips")
data class Trip(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val riderId: UUID,

    @Column(nullable = false)
    val bikeId: UUID,

    @Column(nullable = false)
    val startStationId: UUID,

    @Column
    var destStationId: UUID? = null,

    @Column(nullable = false)
    val startedAt: Instant = Instant.now(),

    @Column
    var endedAt: Instant? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: TripStatus = TripStatus.IN_PROGRESS
)