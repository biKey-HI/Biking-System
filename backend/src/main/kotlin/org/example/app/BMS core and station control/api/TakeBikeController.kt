package org.example.app.bmscoreandstationcontrol.api

import org.example.app.bmscoreandstationcontrol.domain.BikeState
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationEntity
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationService
import org.example.app.bmscoreandstationcontrol.persistence.Trip
import org.example.app.bmscoreandstationcontrol.persistence.TripRepository
import org.example.app.bmscoreandstationcontrol.persistence.TripStatus
import org.example.app.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api")
class TakeBikeController(
    private val stationRepo: DockingStationRepository,
    private val userRepo: UserRepository,
    private val stationSvc: DockingStationService,
    private val tripRepo: TripRepository
) {
    data class TakeBikeRequest(val stationId: String, val userEmail: String)
    data class TakeBikeResponse(val bikeId: String, val tripId: String, val startedAtEpochMs: Long)

    @PostMapping("/take-bike")
    @Transactional
    fun takeBike(@RequestBody req: TakeBikeRequest): TakeBikeResponse {
        val stationEntity = stationRepo.findById(UUID.fromString(req.stationId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found") }

        val user = userRepo.findByEmail(req.userEmail)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        // converting jpa
        val station = stationEntity.toDomain()

        // pick dock that holds an available bike
        val dockWithBike = station.docks.firstOrNull { it.bike != null && it.bike!!.status == BikeState.AVAILABLE }
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "No available bikes at this station")

        val bike = dockWithBike.bike!!

        stationSvc.takeBike(
            dockingStation = station,
            bike = bike,
            fromReservation = false,
            userId = user.id!! // for uuid
        ) ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot take bike in current state") // (takeBike) :contentReference[oaicite:4]{index=4}

        // persist the new state by converting domain to entity
        stationRepo.save(DockingStationEntity(station))

        // Create trip record
        val trip = Trip(
            riderId = user.id!!,
            bikeId = bike.id,
            startStationId = station.id,
            status = TripStatus.IN_PROGRESS
        )
        tripRepo.save(trip)

        // find the time the bike entered ON_TRIP (fallback to now if not present)
        val startedAt = bike.statusTransitions.lastOrNull { it.toState == BikeState.ON_TRIP }?.atTime
            ?: Instant.now()

        return TakeBikeResponse(
            bikeId = bike.id.toString(),
            tripId = trip.id.toString(),
            startedAtEpochMs = startedAt.toEpochMilli()
        )
    }
}

