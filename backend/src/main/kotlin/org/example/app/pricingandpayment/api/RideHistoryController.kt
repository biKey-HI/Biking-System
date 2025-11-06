package org.example.app.pricingandpayment.api

import org.example.app.billing.BillingService
import org.example.app.billing.TripSummaryDTO
import org.example.app.bmscoreandstationcontrol.persistence.TripRepository
import org.example.app.bmscoreandstationcontrol.persistence.TripStatus
import org.example.app.pricingandpayment.api.TripFacade
import org.example.app.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/ride-history")
class RideHistoryController(
    private val tripRepository: TripRepository,
    private val tripFacade: TripFacade,
    private val billingService: BillingService,
    private val userRepository: UserRepository,
    private val bikes: org.example.app.bmscoreandstationcontrol.persistence.BicycleRepository
) {
    private val logger = LoggerFactory.getLogger(RideHistoryController::class.java)

    @GetMapping("/{userId}")
    fun getRideHistory(@PathVariable userId: UUID): List<TripSummaryDTO> {
        logger.info("Fetching ride history for user: $userId")
        
        val user = userRepository.findById(userId).orElseThrow {
            logger.warn("User not found: $userId")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        // Get all trips for the user, ordered by most recent first
        val trips = tripRepository.findByRiderIdOrderByStartedAtDesc(userId)
        
        // Filter only completed trips and convert to TripSummaryDTO
        return trips
            .filter { it.status == TripStatus.COMPLETED && it.endedAt != null }
            .map { trip ->
                try {
                    val tripDomain = tripFacade.getTripDomain(trip.id)
                    billingService.summarize(tripDomain, bikes, user.paymentStrategy)
                } catch (e: Exception) {
                    logger.error("Error processing trip ${trip.id}", e)
                    null
                }
            }
            .filterNotNull()
    }
}

