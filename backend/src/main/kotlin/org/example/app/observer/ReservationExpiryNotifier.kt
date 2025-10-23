package org.example.app.observer

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Concrete notifier for reservation expiry events
 * Monitors bicycle reservations and notifies observers when they're about to expire
 */
@Component
class ReservationExpiryNotifier @Autowired constructor(
    private val appObserver: AppObserver,
    private val reservationHelper: ReservationHelper
) : Notifier {
    private val observers = CopyOnWriteArrayList<Observer>()
    
    init {
        // Assign only App observer for reservation expiry notifications
        observers.add(appObserver)
    }
    
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
    
    override fun notifyObservers(message: String) {
        observers.forEach { observer ->
            try {
                observer.update(message)
            } catch (e: Exception) {
                println("Error notifying observer: ${e.message}")
            }
        }
    }
    
    /**
     * Check if a reservation is about to expire using the helper
     * @param reservationTime The time when the reservation was made
     * @param expiryDurationMinutes The expiry duration in minutes
     * @return true if reservation is about to expire, false otherwise
     */
    fun checkReservationExpiry(reservationTime: Instant, expiryDurationMinutes: Long): Boolean {
        return reservationHelper.checkReservationExpiry(reservationTime, expiryDurationMinutes)
    }
    
    /**
     * Notify observers about reservation expiry
     * @param bikeId The bicycle ID
     * @param timeRemainingMinutes Minutes remaining until expiry
     */
    fun notifyReservationExpiry(bikeId: String, timeRemainingMinutes: Long) {
        val message = "Bicycle $bikeId reservation expires in $timeRemainingMinutes minutes"
        notifyObservers(message)
    }
}
