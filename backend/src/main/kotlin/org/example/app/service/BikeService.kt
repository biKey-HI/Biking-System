package org.example.app.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.example.app.observer.OvertimeNotifier
import org.example.app.observer.TripEndingNotifier
import org.example.app.observer.ReservationExpiryNotifier
import org.example.app.observer.ReservationHelper
import java.time.Instant
import java.time.Duration
import java.util.UUID

/**
 * Service for managing bike operations
 * Integrates with notifiers to send notifications
 */
@Service
class BikeService @Autowired constructor(
    private val overtimeNotifier: OvertimeNotifier,
    private val tripEndingNotifier: TripEndingNotifier,
    private val reservationExpiryNotifier: ReservationExpiryNotifier,
    private val reservationHelper: ReservationHelper
) {
    
    // Mock bike data - in real implementation, this would be from a database
    private val activeTrips = mutableMapOf<String, BikeTrip>()
    private val reservations = mutableMapOf<String, BikeReservation>()
    
    /**
     * Start a bike trip
     * @param bikeId The bicycle ID
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    fun startTrip(bikeId: String, userId: String): Boolean {
        try {
            val trip = BikeTrip(
                bikeId = bikeId,
                userId = userId,
                startTime = Instant.now(),
                startStationId = "station-001" // Mock station ID
            )
            activeTrips[bikeId] = trip
            println("Trip started for bike $bikeId by user $userId")
            return true
        } catch (e: Exception) {
            println("Error starting trip: ${e.message}")
            return false
        }
    }
    
    /**
     * End a bike trip
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID
     * @return true if successful, false otherwise
     */
    fun endTrip(bikeId: String, stationId: String): Boolean {
        try {
            val trip = activeTrips.remove(bikeId) ?: return false
            val duration = Duration.between(trip.startTime, Instant.now())
            val durationMinutes = duration.toMinutes()
            
            // Calculate cost (mock calculation)
            val totalCost = calculateTripCost(durationMinutes)
            
            // Notify about trip ending
            tripEndingNotifier.notifyTripEnding(bikeId, stationId, durationMinutes, totalCost)
            
            println("Trip ended for bike $bikeId at station $stationId after $durationMinutes minutes")
            return true
        } catch (e: Exception) {
            println("Error ending trip: ${e.message}")
            return false
        }
    }
    
    /**
     * Reserve a bike
     * @param bikeId The bicycle ID
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    fun reserveBike(bikeId: String, userId: String): Boolean {
        try {
            val reservation = BikeReservation(
                bikeId = bikeId,
                userId = userId,
                reservationTime = Instant.now(),
                expiryDurationMinutes = 10
            )
            reservations[bikeId] = reservation
            println("Bike $bikeId reserved by user $userId")
            return true
        } catch (e: Exception) {
            println("Error reserving bike: ${e.message}")
            return false
        }
    }
    
    /**
     * Check for overtime bikes and notify
     */
    fun checkOvertimeBikes() {
        activeTrips.forEach { (bikeId, trip) ->
            val duration = Duration.between(trip.startTime, Instant.now())
            val durationMinutes = duration.toMinutes()
            
            // Check if bike is overtime (45+ minutes)
            if (durationMinutes > 45) {
                overtimeNotifier.notifyOvertime(bikeId, durationMinutes)
            }
        }
    }
    
    /**
     * Check for expiring reservations and notify
     */
    fun checkExpiringReservations() {
        reservations.forEach { (bikeId, reservation) ->
            if (reservationHelper.checkReservationExpiry(reservation.reservationTime, reservation.expiryDurationMinutes)) {
                val timeRemaining = reservationHelper.getTimeRemaining(reservation.reservationTime, reservation.expiryDurationMinutes)
                reservationExpiryNotifier.notifyReservationExpiry(bikeId, timeRemaining)
            }
        }
    }
    
    /**
     * Calculate trip cost (mock implementation)
     * @param durationMinutes Trip duration in minutes
     * @return Total cost
     */
    private fun calculateTripCost(durationMinutes: Long): Float {
        val baseCost = 1.50f
        val overtimeRate = 0.20f
        val overtimeMinutes = maxOf(0, durationMinutes - 45)
        
        return baseCost + (overtimeMinutes * overtimeRate / 60)
    }
}

/**
 * Data class for bike trips
 */
data class BikeTrip(
    val bikeId: String,
    val userId: String,
    val startTime: Instant,
    val startStationId: String
)

/**
 * Data class for bike reservations
 */
data class BikeReservation(
    val bikeId: String,
    val userId: String,
    val reservationTime: Instant,
    val expiryDurationMinutes: Long
)
