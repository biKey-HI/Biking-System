package org.example.app.observer

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Service to manage the Observer pattern for bike system notifications
 * Coordinates between notifiers and observers
 */
@Service
class NotificationService @Autowired constructor(
    private val overtimeNotifier: OvertimeNotifier,
    private val reservationExpiryNotifier: ReservationExpiryNotifier,
    private val tripEndingNotifier: TripEndingNotifier,
    private val appObserver: AppObserver,
    private val emailObserver: EmailObserver,
    private val messageTextObserver: MessageTextObserver
) {
    
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    
    init {
        // Attach all observers to all notifiers
        setupObservers()
        
        // Start background monitoring tasks
        startBackgroundMonitoring()
    }
    
    /**
     * Setup observers for all notifiers
     */
    private fun setupObservers() {
        // Attach observers to overtime notifier
        overtimeNotifier.attach(appObserver)
        overtimeNotifier.attach(emailObserver)
        overtimeNotifier.attach(messageTextObserver)
        
        // Attach observers to reservation expiry notifier
        reservationExpiryNotifier.attach(appObserver)
        reservationExpiryNotifier.attach(emailObserver)
        reservationExpiryNotifier.attach(messageTextObserver)
        
        // Attach observers to trip ending notifier
        tripEndingNotifier.attach(appObserver)
        tripEndingNotifier.attach(emailObserver)
        tripEndingNotifier.attach(messageTextObserver)
    }
    
    /**
     * Start background monitoring tasks
     */
    private fun startBackgroundMonitoring() {
        // Monitor for overtime every 5 minutes
        scheduler.scheduleAtFixedRate({
            checkOvertimeBikes()
        }, 0, 5, TimeUnit.MINUTES)
        
        // Monitor for reservation expiry every minute
        scheduler.scheduleAtFixedRate({
            checkReservationExpiry()
        }, 0, 1, TimeUnit.MINUTES)
    }
    
    /**
     * Check for bikes that have exceeded overtime limit
     */
    private fun checkOvertimeBikes() {
        // This would integrate with your bike service to check active trips
        // For now, this is a placeholder
        println("Checking for overtime bikes...")
    }
    
    /**
     * Check for reservations that are about to expire
     */
    private fun checkReservationExpiry() {
        // This would integrate with your bike service to check active reservations
        // For now, this is a placeholder
        println("Checking for expiring reservations...")
    }
    
    /**
     * Notify about bike overtime
     * @param bikeId The bicycle ID
     * @param durationMinutes The duration in minutes
     */
    fun notifyOvertime(bikeId: String, durationMinutes: Long) {
        overtimeNotifier.notifyOvertime(bikeId, durationMinutes)
    }
    
    /**
     * Notify about reservation expiry
     * @param bikeId The bicycle ID
     * @param timeRemainingMinutes Minutes remaining until expiry
     */
    fun notifyReservationExpiry(bikeId: String, timeRemainingMinutes: Long) {
        reservationExpiryNotifier.notifyReservationExpiry(bikeId, timeRemainingMinutes)
    }
    
    /**
     * Notify about trip ending
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID
     * @param tripDurationMinutes The duration of the trip in minutes
     */
    fun notifyTripEnding(bikeId: String, stationId: String, tripDurationMinutes: Long) {
        tripEndingNotifier.notifyTripEnding(bikeId, stationId, tripDurationMinutes)
    }
    
    /**
     * Notify about trip completion
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID
     * @param totalCost The total cost of the trip
     */
    fun notifyTripCompleted(bikeId: String, stationId: String, totalCost: Float) {
        tripEndingNotifier.notifyTripCompleted(bikeId, stationId, totalCost)
    }
    
    /**
     * Add SSE connection for real-time app notifications
     * @param userId The user ID
     * @param emitter The SSE emitter
     */
    fun addAppConnection(userId: String, emitter: SseEmitter) {
        appObserver.addConnection(userId, emitter)
    }
    
    /**
     * Remove SSE connection
     * @param userId The user ID
     */
    fun removeAppConnection(userId: String) {
        appObserver.removeConnection(userId)
    }
    
    /**
     * Add user email for notifications
     * @param userId The user ID
     * @param email The user's email
     */
    fun addUserEmail(userId: String, email: String) {
        emailObserver.addUserEmail(userId, email)
    }
    
    /**
     * Add user phone for SMS notifications
     * @param userId The user ID
     * @param phoneNumber The user's phone number
     */
    fun addUserPhone(userId: String, phoneNumber: String) {
        messageTextObserver.addUserPhone(userId, phoneNumber)
    }
    
    /**
     * Get notification statistics
     * @return Map of notification statistics
     */
    fun getNotificationStats(): Map<String, Any> {
        return mapOf(
            "activeAppConnections" to appObserver.getActiveConnectionCount(),
            "registeredPhones" to messageTextObserver.getRegisteredPhoneCount()
        )
    }
}
