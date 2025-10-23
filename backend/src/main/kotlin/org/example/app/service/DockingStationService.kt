package org.example.app.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.example.app.observer.TripEndingNotifier
import org.example.app.observer.ReservationExpiryNotifier
import org.example.app.observer.ReservationHelper
import java.time.Instant
import java.util.UUID

/**
 * Service for managing docking station operations
 * Integrates with notifiers to send notifications
 */
@Service
class DockingStationService @Autowired constructor(
    private val tripEndingNotifier: TripEndingNotifier,
    private val reservationExpiryNotifier: ReservationExpiryNotifier,
    private val reservationHelper: ReservationHelper
) {
    
    // Mock station data - in real implementation, this would be from a database
    private val stations = mutableMapOf<String, DockingStation>()
    private val bikeReservations = mutableMapOf<String, BikeReservation>()
    
    init {
        // Initialize with some mock stations
        stations["station-001"] = DockingStation(
            id = "station-001",
            name = "Central Station",
            capacity = 20,
            availableDocks = 15
        )
        stations["station-002"] = DockingStation(
            id = "station-002", 
            name = "University Station",
            capacity = 15,
            availableDocks = 10
        )
    }
    
    /**
     * Return a bike to a docking station
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    fun returnBike(bikeId: String, stationId: String, userId: String): Boolean {
        try {
            val station = stations[stationId] ?: return false
            
            // Check if station has available docks
            if (station.availableDocks <= 0) {
                println("Station $stationId is full, cannot return bike $bikeId")
                return false
            }
            
            // Mock trip duration calculation
            val tripDurationMinutes = 25L // Mock duration
            
            // Calculate cost (mock calculation)
            val totalCost = calculateTripCost(tripDurationMinutes)
            
            // Update station capacity
            station.availableDocks--
            station.occupiedDocks++
            
            // Notify about trip ending/completion
            tripEndingNotifier.notifyTripEnding(bikeId, stationId, tripDurationMinutes, totalCost)
            
            println("Bike $bikeId returned to station $stationId by user $userId")
            return true
        } catch (e: Exception) {
            println("Error returning bike: ${e.message}")
            return false
        }
    }
    
    /**
     * Reserve a bike at a station
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    fun reserveBike(bikeId: String, stationId: String, userId: String): Boolean {
        try {
            val station = stations[stationId] ?: return false
            
            // Check if bike is available at this station
            if (station.occupiedDocks <= 0) {
                println("No bikes available at station $stationId")
                return false
            }
            
            val reservation = BikeReservation(
                bikeId = bikeId,
                userId = userId,
                stationId = stationId,
                reservationTime = Instant.now(),
                expiryDurationMinutes = 10
            )
            bikeReservations[bikeId] = reservation
            
            // Update station to mark bike as reserved
            station.availableDocks--
            station.reservedDocks++
            
            println("Bike $bikeId reserved at station $stationId by user $userId")
            return true
        } catch (e: Exception) {
            println("Error reserving bike: ${e.message}")
            return false
        }
    }
    
    /**
     * Take a reserved bike
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    fun takeReservedBike(bikeId: String, stationId: String, userId: String): Boolean {
        try {
            val reservation = bikeReservations.remove(bikeId) ?: return false
            val station = stations[stationId] ?: return false
            
            // Check if reservation is still valid
            if (reservationHelper.checkReservationExpiry(reservation.reservationTime, reservation.expiryDurationMinutes)) {
                val timeRemaining = reservationHelper.getTimeRemaining(reservation.reservationTime, reservation.expiryDurationMinutes)
                reservationExpiryNotifier.notifyReservationExpiry(bikeId, timeRemaining)
            }
            
            // Update station
            station.reservedDocks--
            station.occupiedDocks--
            
            println("Reserved bike $bikeId taken from station $stationId by user $userId")
            return true
        } catch (e: Exception) {
            println("Error taking reserved bike: ${e.message}")
            return false
        }
    }
    
    /**
     * Check for expiring reservations and notify
     */
    fun checkExpiringReservations() {
        bikeReservations.forEach { (bikeId, reservation) ->
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
    
    /**
     * Get station information
     * @param stationId The station ID
     * @return Station information or null if not found
     */
    fun getStation(stationId: String): DockingStation? {
        return stations[stationId]
    }
}

/**
 * Data class for docking stations
 */
data class DockingStation(
    val id: String,
    val name: String,
    val capacity: Int,
    var availableDocks: Int,
    var occupiedDocks: Int = 0,
    var reservedDocks: Int = 0
)

/**
 * Data class for bike reservations
 */
data class BikeReservation(
    val bikeId: String,
    val userId: String,
    val stationId: String,
    val reservationTime: Instant,
    val expiryDurationMinutes: Long
)
