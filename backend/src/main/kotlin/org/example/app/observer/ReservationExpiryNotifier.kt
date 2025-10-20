package org.example.app.observer

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Concrete notifier for reservation expiry events
 * Monitors bicycle reservations and notifies observers when they're about to expire
 */
@Component
class ReservationExpiryNotifier : Notifier {
    private val observers = CopyOnWriteArrayList<Observer>()
    private val EXPIRY_WARNING = 2 // 2 minutes warning before expiry
    
    override fun attach(observer: Observer) {
        observers.add(observer)
    }
    
    override fun detach(observer: Observer) {
        observers.remove(observer)
    }
    
    override fun notifyObservers() {
        observers.forEach { observer ->
            try {
                observer.update("Bicycle reservation is about to expire")
            } catch (e: Exception) {
                println("Error notifying observer: ${e.message}")
            }
        }
    }
    
    /**
     * Check if a reservation is about to expire
     * @param reservationTime The time when the reservation was made
     * @param expiryDurationMinutes The expiry duration in minutes
     * @return true if reservation is about to expire, false otherwise
     */
    fun checkReservationExpiry(reservationTime: Instant, expiryDurationMinutes: Long): Boolean {
        val now = Instant.now()
        val timeUntilExpiry = Duration.between(now, reservationTime.plus(Duration.ofMinutes(expiryDurationMinutes)))
        return timeUntilExpiry.toMinutes() <= EXPIRY_WARNING && timeUntilExpiry.toMinutes() > 0
    }
    
    /**
     * Notify observers about reservation expiry
     * @param bikeId The bicycle ID
     * @param timeRemainingMinutes Minutes remaining until expiry
     */
    fun notifyReservationExpiry(bikeId: String, timeRemainingMinutes: Long) {
        val message = "Bicycle $bikeId reservation expires in $timeRemainingMinutes minutes"
        observers.forEach { observer ->
            try {
                observer.update(message)
            } catch (e: Exception) {
                println("Error notifying observer about reservation expiry: ${e.message}")
            }
        }
    }
}
