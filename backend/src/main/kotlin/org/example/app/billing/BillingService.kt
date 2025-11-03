package org.example.app.billing

import org.example.app.billing.TripSummaryDTO
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

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

    fun summarize(trip: TripDomain): TripSummaryDTO {
        val minutes = Duration.between(trip.startTime, trip.endTime).toMinutes().toInt().coerceAtLeast(0)
        val cost = pricing.price(minutes, trip.isEBike, trip.overtimeCents)
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
