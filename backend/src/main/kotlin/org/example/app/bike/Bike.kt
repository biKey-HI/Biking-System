package org.example.app.bike

import jakarta.persistence.Entity
import java.time.Duration

@Entity
class Bike(
    id: java.util.UUID = java.util.UUID.randomUUID(),
    status: BikeState = BikeState.AVAILABLE,
    baseCost: Float = 1.50f,
    overtimeRate: Float = 0.20f
) : Bicycle(id, status, null, baseCost, overtimeRate) {

    fun isOvertime(duration: Duration): Boolean = duration > Duration.ofMinutes((0.75 * 60).toLong())
}
