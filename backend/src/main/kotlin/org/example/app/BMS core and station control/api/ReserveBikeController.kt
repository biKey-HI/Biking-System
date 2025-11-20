package org.example.app.bmscoreandstationcontrol.api

import org.example.app.bmscoreandstationcontrol.domain.BikeState
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationEntity
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationService
import org.example.app.user.UserRepository
import org.example.app.loyalty.LoyaltyService
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Duration


@RestController
@RequestMapping("/api")
class ReserveBikeController(
    private val stationRepo: DockingStationRepository,
    private val userRepo: UserRepository,
    private val stationSvc: DockingStationService,
    private val loyaltyService: LoyaltyService
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

        // Get user to check loyalty tier
        val user = userRepo.findById(userUuid)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        val station = stationEntity.toDomain()

        // Apply loyalty tier reservation hold time bonus
        val baseHoldMinutes = station.reservationHoldTime.toMinutes()
        val loyaltyBonusMinutes = user.loyaltyTier.reservationHoldExtraMinutes
        val totalHoldMinutes = baseHoldMinutes + loyaltyBonusMinutes
        station.reservationHoldTime = Duration.ofMinutes(totalHoldMinutes)

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
            ?: Instant.now().plusSeconds(totalHoldMinutes * 60).toEpochMilli()

        return ReserveBikeResponse(
            stationId = req.stationId,
            bikeId = req.bikeId,
            userId = req.userId,
            reservedUntilEpochMs = expiryTime
        )
    }
}