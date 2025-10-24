package org.example.app.bike

import jakarta.persistence.*
import java.time.Duration
import java.time.Instant
import java.util.*

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
abstract class Bicycle(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    var status: BikeState = BikeState.AVAILABLE,

    var reservationExpiryTime: Instant? = null,

    var baseCost: Float = 0f,
    var overtimeRate: Float = 0f
) {
    @OneToMany(mappedBy = "bike", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var statusTransitions: MutableList<BikeStateTransition> = mutableListOf()

    open fun startReservation() {
        if (status == BikeState.AVAILABLE) {
            status = BikeState.RESERVED
            reservationExpiryTime = Instant.now().plus(Duration.ofMinutes(10))
            addTransition(BikeState.AVAILABLE, BikeState.RESERVED)
        } else {
            throw IllegalStateException("Bike cannot be reserved in its current state: $status")
        }
    }

    open fun startTrip() {
        if (status == BikeState.RESERVED || status == BikeState.AVAILABLE) {
            status = BikeState.ON_TRIP
            addTransition(BikeState.RESERVED, BikeState.ON_TRIP)
        } else {
            throw IllegalStateException("Cannot start trip: bike is $status")
        }
    }

    open fun endTrip() {
        if (status == BikeState.ON_TRIP) {
            status = BikeState.AVAILABLE
            reservationExpiryTime = null
            addTransition(BikeState.ON_TRIP, BikeState.AVAILABLE)
        } else {
            throw IllegalStateException("Cannot end trip: bike is not on trip")
        }
    }

    private fun addTransition(from: BikeState, to: BikeState) {
        statusTransitions.add(BikeStateTransition(UUID.randomUUID(), this, from, to, Instant.now()))
    }
}
