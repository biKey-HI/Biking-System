package org.example.app.bmscoreandstationcontrol.persistence

import org.example.app.bmscoreandstationcontrol.domain.Bicycle
import org.example.app.bmscoreandstationcontrol.domain.BikeState
import org.example.app.bmscoreandstationcontrol.domain.BikeStateTransition
import org.example.app.bmscoreandstationcontrol.domain.DockState
import org.example.app.bmscoreandstationcontrol.domain.DockingStation
import org.example.app.bmscoreandstationcontrol.domain.DockingStationState
import org.example.app.bmscoreandstationcontrol.domain.OutOfService
import org.example.app.bmscoreandstationcontrol.domain.fail
import org.example.app.bmscoreandstationcontrol.domain.ok
import org.example.app.user.UserRepository
import org.example.app.user.UserRole
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DockingStationService(
    private val userRepository: UserRepository,
    private val bikeRepository: BicycleRepository
) {
    fun bikeIsAvailable(dockingStation: DockingStation): Boolean? = dockingStation.bikeIsAvailable()

    fun takeBike(
        dockingStation: DockingStation,
        bike: Bicycle,
        fromReservation: Boolean = false,
        userId: UUID? = null
    ): Unit? = dockingStation.takeBike(bike, fromReservation, userId, userRepository)

    fun returnBike(
        dockingStation: DockingStation,
        bike: Bicycle,
        dockId: UUID? = null,
        userId: UUID
    ): Unit? = dockingStation.returnBike(bike, dockId, userId, userRepository)

    fun changeStationStatus(
        dockingStation: DockingStation,
        newStatus: DockingStationState
    ): Unit? = dockingStation.changeStationStatus(newStatus)

    fun reserveBike(
        dockingStation: DockingStation,
        bike: Bicycle?,
        userId: UUID
    ): Unit? = dockingStation.reserveBike(bike, userId, userRepository)

    fun updateReservation(dockingStation: DockingStation): Unit? = dockingStation.updateReservation()

    fun moveBikeFromThisStation(
        dockingStation: DockingStation,
        userId: UUID,
        bike: Bicycle,
        toStation: DockingStation,
        toDockId: UUID? = null
    ): Boolean? = dockingStation.moveBikeFromThisStation(userId, bike, toStation, toDockId, userRepository)

    fun toggleOutOfService(dockingStation: DockingStation, userId: UUID): Unit? =
        dockingStation.toggleOutOfService(userId, userRepository)

    fun toggleDockOutOfService(dockingStation: DockingStation,
       userId: UUID,
       dockId: UUID
    ): Unit? = dockingStation.toggleDockOutOfService(userId, dockId, userRepository)

    fun toggleBikeMaintenance(
        dockingStation: DockingStation,
        userId: UUID,
        bikeId: UUID
    ): Unit? = dockingStation.toggleBikeMaintenance(userId, bikeId, userRepository, bikeRepository)
}