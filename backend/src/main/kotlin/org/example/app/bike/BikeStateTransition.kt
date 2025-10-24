package org.example.app.bike

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
data class BikeStateTransition(
    @Id val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id")
    val bike: Bicycle? = null,

    @Enumerated(EnumType.STRING)
    val fromState: BikeState,

    @Enumerated(EnumType.STRING)
    val toState: BikeState,

    val atTime: Instant
)
