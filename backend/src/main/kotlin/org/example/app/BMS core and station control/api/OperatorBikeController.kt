package org.example.app.bmscoreandstationcontrol.api

import org.example.app.bmscoreandstationcontrol.domain.Bike
import org.example.app.bmscoreandstationcontrol.persistence.BicycleEntity
import org.example.app.bmscoreandstationcontrol.persistence.BicycleRepository
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationEntity
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.example.app.user.User
import org.example.app.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.util.UUID

@RestController
@RequestMapping("/api")
class OperatorController(
    private val stationRepo: DockingStationRepository,
    private val userRepo: UserRepository,
    private val bikeRepo: BicycleRepository
) {
    data class MoveBikeRequest(val fromStationId: String, val userId: String, val bikeId: String, val toDockId: String?, val toStationId: String)
    data class ToggleStationOutOfServiceRequest(val dockingStationId: String, val userId: String)
    data class ToggleDockOutOfServiceRequest(val dockingStationId: String, val userId: String, val dockId: String)
    data class ToggleBikeMaintenanceRequest(val dockingStationId: String, val userId: String, val bikeId: String)

    @PostMapping("/move-bike")
    @Transactional
    fun moveBike(@RequestBody req: MoveBikeRequest): Boolean? {
        val fromStationEntity = stationRepo.findById(UUID.fromString(req.fromStationId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "First station not found") }
        val fromStation = fromStationEntity.toDomain()

        val toStationEntity = stationRepo.findById(UUID.fromString(req.toStationId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Second station not found") }
        val toStation = toStationEntity.toDomain()

        val userOpt: Optional<User> = userRepo.findById(UUID.fromString(req.userId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val user: User = userOpt.orElse(null)

        val bike = fromStation.docks.firstOrNull {it.bike?.id == UUID.fromString(req.bikeId)}?.bike
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found in first station")

        val toDock = toStation.docks.firstOrNull {it.id == UUID.fromString(req.toDockId)}

        val result = fromStation.moveBikeFromThisStation(user.id ?: UUID.randomUUID(), bike, toStation, toDock?.id, userRepo)
        result?.let {
            stationRepo.save(DockingStationEntity(fromStation))
            if(result) {
                stationRepo.save(DockingStationEntity(toStation))
            } else {
                bikeRepo.save(BicycleEntity(bike))
            }
        }
        return result
    }

    @PostMapping("/out-of-service-station")
    @Transactional
    fun toggleStationOutOfService(@RequestBody req: ToggleStationOutOfServiceRequest): Unit? {
        val stationEntity = stationRepo.findById(UUID.fromString(req.dockingStationId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found") }
        val station = stationEntity.toDomain()

        val userOpt: Optional<User> = userRepo.findById(UUID.fromString(req.userId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val user: User = userOpt.orElse(null)

        val result = station.toggleOutOfService(user.id ?: UUID.randomUUID(), userRepo)
        result?.let {
            stationRepo.save(DockingStationEntity(station))
        }
        return result
    }

    @PostMapping("/out-of-service-dock")
    @Transactional
    fun toggleDockOutOfService(@RequestBody req: ToggleDockOutOfServiceRequest): Unit? {
        val stationEntity = stationRepo.findById(UUID.fromString(req.dockingStationId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found") }
        val station = stationEntity.toDomain()

        val userOpt: Optional<User> = userRepo.findById(UUID.fromString(req.userId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val user: User = userOpt.orElse(null)

        val dock = station.docks.firstOrNull {it.id == UUID.fromString(req.dockId)}
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Dock not found in station")

        val result = station.toggleDockOutOfService(user.id ?: UUID.randomUUID(), dock.id, userRepo)
        result?.let {
            stationRepo.save(DockingStationEntity(station))
        }
        return result
    }

    @PostMapping("/maintenance-bike")
    @Transactional
    fun toggleBikeMaintenance(@RequestBody req: ToggleBikeMaintenanceRequest): Unit? {
        val stationEntity = stationRepo.findById(UUID.fromString(req.dockingStationId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found") }
        val station = stationEntity.toDomain()

        val userOpt: Optional<User> = userRepo.findById(UUID.fromString(req.userId))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val user: User = userOpt.orElse(null)

        val dock = station.docks.firstOrNull {it.bike?.id == UUID.fromString(req.bikeId)}
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found in station's docks")

        val result = station.toggleBikeMaintenance(user.id ?: UUID.randomUUID(), dock.bike?.id ?: UUID.randomUUID(), userRepo)
        result?.let {
            bikeRepo.save(BicycleEntity(dock.bike ?: Bike()))
            stationRepo.save(DockingStationEntity(station))
        }
        return result
    }
}

