package org.example.app.pricingandpayment.api

import org.example.app.billing.BillingService
import org.example.app.bmscoreandstationcontrol.persistence.BicycleEntity
import org.example.app.bmscoreandstationcontrol.persistence.BicycleRepository
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationEntity
import org.example.app.bmscoreandstationcontrol.persistence.TripRepository
import org.example.app.bmscoreandstationcontrol.persistence.Trip
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
    private val users: UserRepository,
    private val tripRepository: TripRepository
) : TripFacade {

    override fun completeTripAndFetchDomain(
        tripId: UUID,
        destStationId: UUID,
        dockId: String?
    ): BillingService.TripDomain {

        // 1) Load trip and related entities
        val trip = tripRepository.findById(tripId).orElseThrow {
            throw NoSuchElementException("Trip not found: $tripId")
        }

        val bikeEntity = bikes.findById(trip.bikeId).orElseThrow {
            throw NoSuchElementException("Bike not found: ${trip.bikeId}")
        }

        val destEntity = stations.findById(destStationId).orElseThrow {
            throw NoSuchElementException("Station not found: $destStationId")
        }

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
            userId = trip.riderId,
            userRepository = users
        )

        // 5) Update trip fields individually since it's a data class
        tripRepository.save(trip.copy(
            endedAt = Instant.now(),
            destStationId = destStationId,
            status = org.example.app.bmscoreandstationcontrol.persistence.TripStatus.COMPLETED
        ))

        // 6) Persist updated bike/station
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

        // 7) Get start station name
        val startStation = stations.findById(trip.startStationId).orElseThrow()


        // Return actual trip data instead of building it
        return BillingService.TripDomain(
            id = trip.id,
            riderId = trip.riderId,
            bikeId = trip.bikeId,
            startStationName = startStation.name,
            endStationName = destStation.name,
            startTime = trip.startedAt,
            endTime = trip.endedAt ?: Instant.now(),
            isEBike = bike is EBike,
            overtimeCents = bike.getOvertimeCost()?.let { (it * 100).toInt() } ?: 0
        )
    }


    override fun getTripDomain(tripId: UUID): BillingService.TripDomain {
        val trip = tripRepository.findById(tripId).orElseThrow {
            throw NoSuchElementException("Trip not found: $tripId")
        }
        val bike = bikes.findById(trip.bikeId).orElseThrow().toDomain()
        val startStation = stations.findById(trip.startStationId).orElseThrow()
        val endStation = trip.destStationId?.let { stations.findById(it).orElse(null) }

        return BillingService.TripDomain(
            id = trip.id,
            riderId = trip.riderId,
            bikeId = trip.bikeId,
            startStationName = startStation.name,
            endStationName = endStation?.name ?: "Unknown",
            startTime = trip.startedAt,
            endTime = trip.endedAt ?: Instant.now(),
            isEBike = bike is EBike,
            overtimeCents = bike.getOvertimeCost()?.let { (it * 100).toInt() } ?: 0
        )
    }

    // helpers

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
            startStationName = "Unknown",
            endStationName = endStationName,
            startTime = start,
            endTime = end,
            isEBike = isEBike,
            overtimeCents = overtimeCents
        )
    }
}
