package org.example.app.bmscoreandstationcontrol.domain

import org.example.app.user.PaymentStrategyType
import java.time.Duration
import java.time.Instant
import java.util.UUID

val missing: Float? = null
val none: Duration? = null

// -- BICYCLES -- \\

abstract class Bicycle(
    open val id: UUID = UUID.randomUUID(),
    open var status: BikeState = BikeState.AVAILABLE,
    open var statusTransitions: MutableList<BikeStateTransition> = mutableListOf(),
    open var reservationExpiryTime: Instant? = if(status == BikeState.RESERVED) Instant.now().plus(Duration.ofMinutes(10)) else null
) {
    abstract val baseCost: Float
    abstract val overtimeRate: Float

    abstract fun isOvertime(): Boolean?

    fun getDuration(): Duration? {
        if(!statusTransitions.isEmpty() && status == BikeState.ON_TRIP) {
            val takenAt = statusTransitions.last().atTime
            return Duration.between(takenAt, Instant.now())
        } else if (statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP) {
            val takenAt = statusTransitions[statusTransitions.size - 2].atTime
            val returnedAt = statusTransitions.last().atTime
            return Duration.between(takenAt, returnedAt)
        } else
            return none
    }

    abstract fun getOvertimeDuration(): Duration?

    fun calculateCost(pricingPlan: PaymentStrategyType): Float? { // Template method
        if (!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP) {
            var cost = getRegularCost(pricingPlan)!!

            if (isOvertime()!!) {
                cost += getOvertimeCost()!!
            }

            return cost
        } else return missing
    }

    abstract fun getRegularCost(pricingPlan: PaymentStrategyType): Float?
    abstract fun getOvertimeCost(): Float?
}

data class Bike(override val id: UUID = UUID.randomUUID(),
    override var status: BikeState = BikeState.AVAILABLE,
    override var statusTransitions: MutableList<BikeStateTransition> = mutableListOf(),
    override var reservationExpiryTime: Instant? = null,
    override val baseCost: Float = 1f,
    override val overtimeRate: Float = 0.20f
) : Bicycle(id, status, statusTransitions, reservationExpiryTime) {
    override fun isOvertime(): Boolean? {
        return if (!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP)
            getDuration()!! > Duration.ofMinutes((0.75*60).toLong())
        else neither
    }

    override fun getOvertimeDuration(): Duration? {
        return if ((!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP)
            && isOvertime()!!)
            getDuration()!! - Duration.ofMinutes((0.75*60).toLong())
        else none
    }

    override fun getRegularCost(pricingPlan: PaymentStrategyType): Float? {
        return if (!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP) {
            if(pricingPlan == PaymentStrategyType.DEFAULT_PAY_NOW) baseCost else 0f
        }
        else missing
    }

    override fun getOvertimeCost(): Float? {
        if ((!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP) && isOvertime()!!) {
            val overtimeMinutes = getOvertimeDuration()!!.toMinutes().toFloat()
            return overtimeRate*overtimeMinutes
        } else return missing
    }
}

data class EBike(override val id: UUID = UUID.randomUUID(),
    override var status: BikeState = BikeState.AVAILABLE,
    override var statusTransitions: MutableList<BikeStateTransition> = mutableListOf(),
    override var reservationExpiryTime: Instant? = null,
    override val baseCost: Float = 0.75f,
    override val overtimeRate: Float = 0.10f,
    val baseRate: Float = 0.20f
) : Bicycle(id, status, statusTransitions, reservationExpiryTime) {
    override fun isOvertime(): Boolean? {
        return if (!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP)
            getDuration()!! > Duration.ofHours(2)
        else neither
    }

    override fun getOvertimeDuration(): Duration? {
        return if ((!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP)
            && isOvertime()!!)
            getDuration()!! - Duration.ofHours(2)
        else none
    }

    override fun getRegularCost(pricingPlan: PaymentStrategyType): Float? {
        return if (!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP) {
            if(pricingPlan == PaymentStrategyType.DEFAULT_PAY_NOW) baseCost else {0f} + baseRate * (getDuration()!!.toMinutes().toFloat()/60)
        }
        else missing
    }

    override fun getOvertimeCost(): Float? {
        if ((!statusTransitions.isEmpty() && status == BikeState.ON_TRIP || statusTransitions.size > 1 && statusTransitions[statusTransitions.size - 2].toState == BikeState.ON_TRIP)
            && isOvertime()!!) {
            val overtimeMinutes = getOvertimeDuration()!!.toMinutes().toFloat()
            return overtimeRate*overtimeMinutes
        } else return missing
    }
}


// -- STATUSES -- \\

enum class BikeState(val displayName: String) {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    ON_TRIP("On Trip"),
    MAINTENANCE("Maintenance");

    override fun toString() = displayName

    companion object {
        fun fromString(name: String): BikeState {
            return entries.find { it.displayName == name }
                ?: throw IllegalArgumentException("Unknown BikeState: $name")
        }
    }
}

data class BikeStateTransition(val forBikeId: UUID,
    val fromState: BikeState,
    val toState: BikeState,
    val atTime: Instant
)
