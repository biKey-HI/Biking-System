package org.example.app.bike

import jakarta.persistence.Entity
import java.time.Duration

@Entity
class EBike(
    id: java.util.UUID = java.util.UUID.randomUUID(),
    status: BikeState = BikeState.AVAILABLE,
    baseCost: Float = 0.75f,
    overtimeRate: Float = 0.10f,
    var baseRate: Float = 0.30f
) : Bicycle(id, status, null, baseCost, overtimeRate) {

    fun isOvertime(duration: Duration): Boolean = duration > Duration.ofHours(2)
}
