package org.example.app.reservation

import org.example.app.bike.BicycleRepository
import org.example.app.bike.BikeState
import org.example.app.user.User
import org.example.app.user.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    // Use the renamed repository
    private val bicycleRepository: BicycleRepository
) {

    @Transactional
    fun createReservation(user: User, bikeId: UUID): Reservation {
        // Ensure the user has the RIDER role before allowing a reservation
        if (user.role != UserRole.RIDER) {
            throw IllegalStateException("Only users with the RIDER role can make reservations.")
        }

        // Check if this user already has an active reservation
        val existingReservation = reservationRepository.findFirstByUserAndStatusOrderByIdDesc(user, "ACTIVE")
        if (existingReservation != null) {
            throw IllegalStateException("User already has an active reservation.")
        }

        // Find the bike by its ID, or throw an exception if not found
        val bike = bicycleRepository.findById(bikeId)
            .orElseThrow { IllegalStateException("Bike with ID $bikeId not found.") }

        // Use the method on the Bicycle object to handle the state transition
        bike.startReservation()

        // Create the reservation record, linking it to the user and the bike
        val reservation = Reservation(user = user, bike = bike, status = "ACTIVE")
        reservationRepository.save(reservation)

        // Save the updated state of the bike to the database
        bicycleRepository.save(bike)

        return reservation
    }

    @Transactional
    fun cancelReservation(user: User) {
        // Find the user's active reservation
        val activeReservation = reservationRepository.findFirstByUserAndStatusOrderByIdDesc(user, "ACTIVE")
            ?: throw IllegalStateException("No active reservation to cancel.")

        // Update the reservation status
        activeReservation.status = "CANCELED"
        reservationRepository.save(activeReservation)

        // Make the bike available again
        val bike = activeReservation.bike
        if (bike.status == BikeState.RESERVED) {
            bike.status = BikeState.AVAILABLE
            bike.reservationExpiryTime = null
            // Save the bike's updated state
            bicycleRepository.save(bike)
        }
    }
}