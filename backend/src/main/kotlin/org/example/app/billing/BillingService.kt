package org.example.app.billing

import org.example.app.bmscoreandstationcontrol.domain.Bicycle
import org.example.app.bmscoreandstationcontrol.persistence.BicycleEntity
import org.example.app.bmscoreandstationcontrol.persistence.BicycleRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID
import org.example.app.user.PaymentStrategyType
import java.util.Optional

@Service
class BillingService(private val pricing: PricingService) {

    // Minimal domain “view” of a completed trip
    data class TripDomain(
        val id: UUID,
        val riderId: UUID,
        val bikeId: UUID,
        val startStationName: String,
        val endStationName: String,
        val startTime: java.time.Instant,
        val endTime: java.time.Instant,
        val isEBike: Boolean,
        val overtimeCents: Int
    )

    fun summarize(trip: TripDomain, bikes: BicycleRepository, pricingPlan: PaymentStrategyType): TripSummaryDTO {
        val minutes = Duration.between(trip.startTime, trip.endTime).toMinutes().toInt().coerceAtLeast(0)
        val bikeOpt: Optional<BicycleEntity>? = bikes.findById(trip.bikeId)
        val bike: BicycleEntity? = bikeOpt?.orElse(null)
        val cost = bike?.let {pricing.price(bike.toDomain(), pricingPlan)} ?: CostBreakdownDTO(0, 0, 0, 0, 0, 0)
        return TripSummaryDTO(
            tripId = trip.id,
            riderId = trip.riderId,
            bikeId = trip.bikeId,
            startStationName = trip.startStationName,
            endStationName = trip.endStationName,
            startTime = trip.startTime,
            endTime = trip.endTime,
            durationMinutes = minutes,
            isEBike = trip.isEBike,
            cost = cost
        )
    }
}
