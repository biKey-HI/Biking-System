package org.example.app.billing

import java.time.Instant
import java.util.UUID

data class CostBreakdownDTO(
    val baseCents: Int,
    val perMinuteCents: Int,
    val minutes: Int,
    val eBikeSurchargeCents: Int?=null,
    val overtimeCents: Int?=null,
    val totalCents: Int
)

data class TripSummaryDTO(
    val tripId: UUID,
    val riderId: UUID,
    val bikeId: UUID,
    val startStationName: String,
    val endStationName: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int,
    val isEBike: Boolean,
    val cost: CostBreakdownDTO
)

data class ReturnAndSummaryResponse(
    val summary: TripSummaryDTO,
    val paymentStrategy: String,
    val requiresImmediatePayment: Boolean,
    val hasSavedCard: Boolean,
    val savedCardLast4: String? = null,
    val provider: String? = null
)

// added for ride history billing info
data class RideHistoryItemDTO(
    val summary: TripSummaryDTO,
    val paymentStrategy: String,
    val hasSavedCard: Boolean,
    val cardHolderName: String? = null,
    val savedCardLast4: String? = null,
    val provider: String? = null
)
