package org.example.app.bmscoreandstationcontrol.persistence

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.example.app.bmscoreandstationcontrol.domain.BikeState
import org.example.app.bmscoreandstationcontrol.domain.BikeStateTransition
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "bike_state_transitions")
data class BikeStateTransitionEntity(
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne
    @JoinColumn(name = "bike_id", nullable = true)
    var bike: BicycleEntity? = null,

    @Column(nullable = false)
    @Convert(converter = BikeStateConverter::class)
    val fromState: BikeState,

    @Column(nullable = false)
    @Convert(converter = BikeStateConverter::class)
    val toState: BikeState,

    @Column(nullable = false)
    val atTime: Instant = Instant.now()
) {
    constructor(transition: BikeStateTransition) : this(
        bike = null,
        fromState = transition.fromState,
        toState = transition.toState,
        atTime = transition.atTime
    )

    fun toDomain(): BikeStateTransition = BikeStateTransition(
        forBikeId = bike?.id ?: UUID.randomUUID(),
        fromState = fromState,
        toState = toState,
        atTime = atTime
    )

    fun toResponse(): org.example.app.bmscoreandstationcontrol.api.BikeStateTransitionResponse =
        org.example.app.bmscoreandstationcontrol.api.BikeStateTransitionResponse(
            forBikeId = bike?.id?.toString() ?: UUID.randomUUID().toString(),
            fromState = fromState.displayName,
            toState = toState.displayName,
            atTime = atTime.toString()
        )
}