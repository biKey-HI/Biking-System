package org.example.app.model

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.example.app.observer.OvertimeNotifier
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Backend Bicycle model with overtime checking logic
 * Integrates with OvertimeNotifier for notifications
 */
@Component
class Bicycle @Autowired constructor(
    private val overtimeNotifier: OvertimeNotifier
) {
    
    // Mock bike data - in real implementation, this would be from a database
    private val bikes = mutableMapOf<String, BikeData>()
    
    init {
        // Initialize with some mock bikes
        bikes["bike-001"] = BikeData(
            id = "bike-001",
            type = BikeType.REGULAR,
            status = BikeStatus.AVAILABLE,
            startTime = null
        )
        bikes["bike-002"] = BikeData(
            id = "bike-002",
            type = BikeType.ELECTRIC,
            status = BikeStatus.ON_TRIP,
            startTime = Instant.now().minus(Duration.ofMinutes(50)) // Mock overtime bike
        )
    }
    
    /**
     * Start a bike trip
     * @param bikeId The bicycle ID
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    fun startTrip(bikeId: String, userId: String): Boolean {
        try {
            val bike = bikes[bikeId] ?: return false
            
            if (bike.status != BikeStatus.AVAILABLE) {
                println("Bike $bikeId is not available")
                return false
            }
            
            bike.status = BikeStatus.ON_TRIP
            bike.startTime = Instant.now()
            bike.userId = userId
            
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
     * @return true if successful, false otherwise
     */
    fun endTrip(bikeId: String): Boolean {
        try {
            val bike = bikes[bikeId] ?: return false
            
            if (bike.status != BikeStatus.ON_TRIP) {
                println("Bike $bikeId is not on a trip")
                return false
            }
            
            bike.status = BikeStatus.AVAILABLE
            bike.startTime = null
            bike.userId = null
            
            println("Trip ended for bike $bikeId")
            return true
        } catch (e: Exception) {
            println("Error ending trip: ${e.message}")
            return false
        }
    }
    
    /**
     * Check if a bike is overtime
     * @param bikeId The bicycle ID
     * @return true if overtime, false otherwise
     */
    fun isOvertime(bikeId: String): Boolean {
        val bike = bikes[bikeId] ?: return false
        
        if (bike.status != BikeStatus.ON_TRIP || bike.startTime == null) {
            return false
        }
        
        val duration = Duration.between(bike.startTime, Instant.now())
        val durationMinutes = duration.toMinutes()
        
        // Check overtime based on bike type
        val overtimeLimit = when (bike.type) {
            BikeType.REGULAR -> 45L // 45 minutes for regular bikes
            BikeType.ELECTRIC -> 120L // 2 hours for electric bikes
        }
        
        return durationMinutes > overtimeLimit
    }
    
    /**
     * Get trip duration in minutes
     * @param bikeId The bicycle ID
     * @return Duration in minutes, or null if not on trip
     */
    fun getTripDurationMinutes(bikeId: String): Long? {
        val bike = bikes[bikeId] ?: return null
        
        if (bike.status != BikeStatus.ON_TRIP || bike.startTime == null) {
            return null
        }
        
        val duration = Duration.between(bike.startTime, Instant.now())
        return duration.toMinutes()
    }
    
    /**
     * Check for overtime bikes and notify
     * This method should be called periodically to check for overtime bikes
     */
    fun checkOvertimeBikes() {
        bikes.forEach { (bikeId, bike) ->
            if (bike.status == BikeStatus.ON_TRIP && bike.startTime != null) {
                val duration = Duration.between(bike.startTime, Instant.now())
                val durationMinutes = duration.toMinutes()
                
                if (isOvertime(bikeId)) {
                    overtimeNotifier.notifyOvertime(bikeId, durationMinutes)
                }
            }
        }
    }
    
    /**
     * Get bike information
     * @param bikeId The bicycle ID
     * @return Bike data or null if not found
     */
    fun getBike(bikeId: String): BikeData? {
        return bikes[bikeId]
    }
    
    /**
     * Get all bikes
     * @return Map of all bikes
     */
    fun getAllBikes(): Map<String, BikeData> {
        return bikes.toMap()
    }
}

/**
 * Data class for bike information
 */
data class BikeData(
    val id: String,
    val type: BikeType,
    var status: BikeStatus,
    var startTime: Instant?,
    var userId: String? = null
)

/**
 * Enum for bike types
 */
enum class BikeType {
    REGULAR,
    ELECTRIC
}

/**
 * Enum for bike status
 */
enum class BikeStatus {
    AVAILABLE,
    ON_TRIP,
    MAINTENANCE,
    RESERVED
}
