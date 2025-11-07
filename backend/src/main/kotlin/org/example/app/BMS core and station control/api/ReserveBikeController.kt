package org.example.app.bmscoreandstationcontrol.api

import org.example.app.bmscoreandstationcontrol.domain.BikeState
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationEntity
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationService
import org.example.app.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@RestController
@RequestMapping("/api")
class ReserveBikeController(
    private val stationRepo: DockingStationRepository,
    private val userRepo: UserRepository,
    private val stationSvc: DockingStationService
) {

    data class ReserveBikeRequest(
        val stationId: String,
        val bikeId: String,
        val userId: String
    )

    data class ReserveBikeResponse(
        val stationId: String,
        val bikeId: String,
        val userId: String,
        val reservedUntilEpochMs: Long
    )

    @PostMapping("/reserve-bike")
    @Transactional
    fun reserveBike(@RequestBody req: ReserveBikeRequest): ReserveBikeResponse {
        val stationEntity = stationRepo.findById(UUID.fromString(req.stationId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found") }

        val userUuid = try {
            UUID.fromString(req.userId)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user UUID")
        }

        val station = stationEntity.toDomain()

        // Try to find the dock containing that bike
        val dockWithBike = station.docks.firstOrNull { it.bike?.id?.toString() == req.bikeId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found in this station")

        val bike = dockWithBike.bike!!

        if (bike.status != BikeState.AVAILABLE) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Bike not available for reservation")
        }

        // Reserve the bike for this user
        stationSvc.reserveBike(
            dockingStation = station,
            bike = bike,
            userId = userUuid
        ) ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Could not reserve bike")

        // Save new state
        stationRepo.save(DockingStationEntity(station))

        val expiryTime = bike.reservationExpiryTime?.toEpochMilli()
            ?: Instant.now().plusSeconds(15 * 60).toEpochMilli() // fallback 15 minutes

        return ReserveBikeResponse(
            stationId = req.stationId,
            bikeId = req.bikeId,
            userId = req.userId,
            reservedUntilEpochMs = expiryTime
        )
    }
}