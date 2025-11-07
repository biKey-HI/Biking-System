package org.example.app.bmscoreandstationcontrol.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.example.app.bmscoreandstationcontrol.domain.DockingStation
import org.example.app.bmscoreandstationcontrol.domain.DockingStationState
import org.example.app.bmscoreandstationcontrol.domain.DockingStationStateTransition
import org.example.app.bmscoreandstationcontrol.domain.OutOfService
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "docking_station_state_transitions")
data class DockingStationStateTransitionEntity(
    @Id
    @Column(columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "station_id", nullable = true, columnDefinition = "CHAR(36)")
    val forStationId: UUID? = null,

    @Column(name = "from_state", nullable = false)
    val fromState: String,

    @Column(name = "to_state", nullable = false)
    val toState: String,

    @Column(name = "at_time", nullable = false)
    val atTime: Instant = Instant.now()
) {
    constructor(domain: DockingStationStateTransition) : this(
        id = UUID.randomUUID(),
        forStationId = domain.forStationId,
        fromState = domain.fromState.toString(),
        toState = domain.toState.toString(),
        atTime = domain.atTime
    )

    fun toDomain(station: DockingStation): DockingStationStateTransition =
        DockingStationStateTransition(
            forStationId = forStationId ?: UUID.randomUUID(),
            fromState = DockingStationState.Companion.valueOf(station, fromState) ?: OutOfService(
                station
            ),
            toState = DockingStationState.Companion.valueOf(station, toState) ?: OutOfService(
                station
            ),
            atTime = atTime
        )

    fun toResponse(): org.example.app.bmscoreandstationcontrol.api.DockingStationStateTransitionResponse =
        org.example.app.bmscoreandstationcontrol.api.DockingStationStateTransitionResponse(
            forStationId = forStationId?.toString() ?: UUID.randomUUID().toString(),
            fromState = fromState,
            toState = toState,
            atTime = atTime.toString()
        )
}