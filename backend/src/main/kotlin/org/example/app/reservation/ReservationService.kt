package org.example.app.reservation

import org.example.app.bike.BicycleRepository
import org.example.app.user.Rider
import org.example.app.bike.Bike
import org.example.app.bike.BikeState
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val bicycleRepository: BicycleRepository

) {

    @Transactional
    fun createReservation(rider: Rider, bikeId: UUID): Reservation {
        val existingReservation = reservationRepository.findFirstByRiderAndStatusOrderByIdDesc(rider, "ACTIVE")
        if (existingReservation != null) {
            throw IllegalStateException("Rider already has an active reservation.")
        }

        val bike = bicycleRepository.findById(bikeId)
            .orElseThrow { IllegalStateException("Bike with ID $bikeId not found.") }

        bike.startReservation()

        val reservation = Reservation(rider = rider, bike = bike, status = "ACTIVE")
        reservationRepository.save(reservation)

        bicycleRepository.save(bike)

        return reservation
    }

    @Transactional
    fun cancelReservation(rider: Rider) {
        val activeReservation = reservationRepository.findFirstByRiderAndStatusOrderByIdDesc(rider, "ACTIVE")
            ?: throw IllegalStateException("No active reservation to cancel.")

        activeReservation.status = "CANCELED"
        reservationRepository.save(activeReservation)

        val bike = activeReservation.bike
        if (bike.status == BikeState.RESERVED) {
            bike.status = BikeState.AVAILABLE
            bike.reservationExpiryTime = null
            bicycleRepository.save(bike)
        }
    }
}