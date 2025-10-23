package org.example.app.observer

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Duration

/**
 * Helper class for reservation expiry checking
 * Contains logic moved from ReservationExpiryNotifier
 */
@Component
class ReservationHelper {
    private val EXPIRY_WARNING = 2 // 2 minutes warning before expiry
    
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
     * Get the time remaining until reservation expiry in minutes
     * @param reservationTime The time when the reservation was made
     * @param expiryDurationMinutes The expiry duration in minutes
     * @return Minutes remaining until expiry, or 0 if already expired
     */
    fun getTimeRemaining(reservationTime: Instant, expiryDurationMinutes: Long): Long {
        val now = Instant.now()
        val timeUntilExpiry = Duration.between(now, reservationTime.plus(Duration.ofMinutes(expiryDurationMinutes)))
        return maxOf(0, timeUntilExpiry.toMinutes())
    }
}
