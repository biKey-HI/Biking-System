package org.example.app.pricingandpayment.api

import org.example.app.billing.BillingService
import org.example.app.bmscoreandstationcontrol.persistence.BicycleEntity
import org.example.app.bmscoreandstationcontrol.persistence.BicycleRepository
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationEntity
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.example.app.bmscoreandstationcontrol.domain.Bicycle
import org.example.app.bmscoreandstationcontrol.domain.BikeState
import org.example.app.bmscoreandstationcontrol.domain.EBike
import org.example.app.bmscoreandstationcontrol.domain.DockingStation
import org.example.app.user.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class TripFacadeImpl(
    private val bikes: BicycleRepository,
    private val stations: DockingStationRepository,
    private val users: UserRepository
) : TripFacade {

    override fun completeTripAndFetchDomain(
        tripId: UUID,
        destStationId: UUID,
        dockId: String?
    ): BillingService.TripDomain {
        // 1) Load from persistence
        val bikeEntity: BicycleEntity = bikes.findById(tripId).orElseThrow()
        val destEntity: DockingStationEntity = stations.findById(destStationId).orElseThrow()

        // 2) Map to domain
        val bike: Bicycle = bikeEntity.toDomain()
        val destStation: DockingStation = destEntity.toDomain()

        // 3) Execute domain return use case
        // need to change if we expect UUID? for dockId - not sure abt that
        // val dockUuid = dockId?.let { UUID.fromString(it) }
        // destStation.status?.returnBike(bike, dockUuid, /*userId*/ null, users)

        // went with String? expected
        val dockUuid: UUID? = dockId
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

        destStation.status?.returnBike(
            bike = bike,
            dockId = dockUuid,
            userId = null,
            userRepository = users
        )

        // 4) Persist updated bike/station
        bikes.save(BicycleEntity(bike))
        stations.save(destEntity.copy(
            name = destStation.name,
            status = destStation.status.toString(),
            capacity = destStation.capacity,
            numFreeDocks = destStation.numFreeDocks,
            numOccupiedDocks = destStation.numOccupiedDocks,
            aBikeIsReserved = destStation.aBikeIsReserved,
            reservationHoldTime = destStation.reservationHoldTime.toMinutes()
        ))

        // 5) Build TripDomain for billing
        return buildTripDomainAfterReturn(
            tripId = tripId,
            bike = bike,
            endStationName = destStation.name
        )
    }

    override fun getTripDomain(tripId: UUID): BillingService.TripDomain {
        val bikeEntity = bikes.findById(tripId).orElseThrow()
        val bike = bikeEntity.toDomain()
        return buildTripDomainAfterReturn(
            tripId = tripId,
            bike = bike,
            endStationName = "Unknown"
        )
    }

    // ---------- helpers ----------

    private fun buildTripDomainAfterReturn(
        tripId: UUID,
        bike: Bicycle,
        endStationName: String
    ): BillingService.TripDomain {
        val start = bike.statusTransitions
            .asReversed()
            .firstOrNull { it.toState == BikeState.ON_TRIP }
            ?.atTime ?: Instant.now().minusSeconds(5 * 60)

        val end = bike.statusTransitions
            .asReversed()
            .firstOrNull { it.toState == BikeState.AVAILABLE }
            ?.atTime ?: Instant.now()

        val isEBike = (bike is EBike)
        val overtimeCents = ((bike.getOvertimeCost() ?: 0f) * 100).toInt()

        return BillingService.TripDomain(
            id = tripId,
            riderId = UUID.nameUUIDFromBytes("rider".toByteArray()), // replace with real riderId when available
            bikeId = bike.id,
            startStationName = "Unknown", // fill from your trip store if you track it
            endStationName = endStationName,
            startTime = start,
            endTime = end,
            isEBike = isEBike,
            overtimeCents = overtimeCents
        )
    }
}
