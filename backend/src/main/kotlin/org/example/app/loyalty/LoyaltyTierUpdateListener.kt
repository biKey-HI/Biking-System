package org.example.app.loyalty

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Event published when a trip is completed
 */
data class TripCompletedEvent(
    val tripId: UUID,
    val riderId: UUID
)

/**
 * Listener that automatically updates loyalty tiers when trips are completed
 */
@Component
class LoyaltyTierUpdateListener(
    private val loyaltyService: LoyaltyService
) {
    private val logger = LoggerFactory.getLogger(LoyaltyTierUpdateListener::class.java)

    @Async
    @EventListener
    fun handleTripCompleted(event: TripCompletedEvent) {
        try {
            logger.info("Trip ${event.tripId} completed by rider ${event.riderId}. Updating loyalty tier...")
            loyaltyService.updateLoyaltyTier(event.riderId)
        } catch (e: Exception) {
            logger.error("Failed to update loyalty tier for rider ${event.riderId}", e)
        }
    }
}

