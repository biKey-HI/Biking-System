package com.example.bikey.ui.bmscoreandstationcontrol.model

import java.time.Duration
import java.time.Instant
import java.util.UUID

// -- BICYCLES -- \\

abstract class Bicycle(
    open val id: UUID = UUID.randomUUID(),
    open var status: BikeState = BikeState.AVAILABLE,
    open var statusTransitions: MutableList<BikeStateTransition> = mutableListOf<BikeStateTransition>(),
    open var reservationExpiryTime: Instant? = if(status == BikeState.RESERVED) Instant.now().plus(Duration.ofMinutes(10)) else null
) {
    abstract val baseCost: Float
    abstract val overtimeRate: Float

    abstract fun isOvertime(): Boolean?

    fun getDuration(): Duration? {
        if (status == BikeState.ON_TRIP) {
            val takenAt = statusTransitions.last().atTime
            return Duration.between(takenAt, Instant.now())
        } else return fail
    }

    abstract fun getOvertimeDuration(): Duration?

    fun calculateCost(): Float? { // Template method
        if (status == BikeState.ON_TRIP) {
            var cost = getRegularCost()!!

            if (isOvertime()!!) {
                cost += getOvertimeCost()!!
            }

            return cost
        } else return fail
    }

    abstract fun getRegularCost(): Float?
    abstract fun getOvertimeCost(): Float?
}

data class Bike(override val id: UUID = UUID.randomUUID(),
    override var status: BikeState = BikeState.AVAILABLE,
    override var statusTransitions: MutableList<BikeStateTransition> = mutableListOf<BikeStateTransition>(),
    override var reservationExpiryTime: Instant? = null,
    override val baseCost: Float = 1.50f,
    override val overtimeRate: Float = 0.20f
) : Bicycle(id, status, statusTransitions, reservationExpiryTime) {
    override fun isOvertime(): Boolean? {
        if (status == BikeState.ON_TRIP)
            return getDuration()!! > Duration.ofMinutes((0.75*60).toLong())
        else return fail
    }

    override fun getOvertimeDuration(): Duration? {
        if (status == BikeState.ON_TRIP && isOvertime()!!)
            return getDuration()!! - Duration.ofMinutes((0.75*60).toLong())
        else return fail
    }

    override fun getRegularCost(): Float? {
        if (status == BikeState.ON_TRIP) return baseCost
        else return fail
    }

    override fun getOvertimeCost(): Float? {
        if (status == BikeState.ON_TRIP && isOvertime()!!) {
            val overtimeHours = getOvertimeDuration()!!.toMinutes().toFloat()/60
            val fractional = getOvertimeDuration()!!.toMinutes().toFloat()%60/60
            return overtimeRate*overtimeHours + baseCost*fractional
        } else return fail
    }
}

data class EBike(override val id: UUID = UUID.randomUUID(),
    override var status: BikeState = BikeState.AVAILABLE,
    override var statusTransitions: MutableList<BikeStateTransition> = mutableListOf<BikeStateTransition>(),
    override var reservationExpiryTime: Instant? = null,
    override val baseCost: Float = 0.75f,
    override val overtimeRate: Float = 0.10f,
    val baseRate: Float = 0.30f
) : Bicycle(id, status, statusTransitions, reservationExpiryTime) {
    override fun isOvertime(): Boolean? {
        if (status == BikeState.ON_TRIP)
            return getDuration()!! > Duration.ofHours(2)
        else return fail
    }

    override fun getOvertimeDuration(): Duration? {
        if (status == BikeState.ON_TRIP && isOvertime()!!)
            return getDuration()!! - Duration.ofHours(2)
        else return fail
    }

    override fun getRegularCost(): Float? {
        if (status == BikeState.ON_TRIP)
            return baseCost + baseRate * (getDuration()!!.toMinutes().toFloat()/60)
        else return fail
    }

    override fun getOvertimeCost(): Float? {
        if (status == BikeState.ON_TRIP && isOvertime()!!) {
            val overtimeHours = getOvertimeDuration()!!.toMinutes().toFloat()/60
            val fractional = getOvertimeDuration()!!.toMinutes().toFloat()%30/30
            return overtimeRate*overtimeHours + baseCost*fractional
        } else return fail
    }
}


// -- STATUSES -- \\

enum class BikeState(val displayName: String) {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    ON_TRIP("On Trip"),
    MAINTENANCE("Maintenance");

    override fun toString() = displayName}

data class BikeStateTransition(val forBikeId: UUID,
    val fromState: BikeState,
    val toState: BikeState,
    val atTime: Instant
)