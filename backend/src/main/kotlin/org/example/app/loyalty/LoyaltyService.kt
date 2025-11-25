package org.example.app.loyalty

import org.example.app.bmscoreandstationcontrol.persistence.TripRepository
import org.example.app.bmscoreandstationcontrol.persistence.TripStatus
import org.example.app.user.User
import org.example.app.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class LoyaltyService(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(LoyaltyService::class.java)

    /**
     * Check if a rider qualifies for Bronze tier:
     * BR-001: Rider has to have no missed reservations within the last year
     * BR-002: Rider returned all bikes that they ever took successfully (lifetime requirement)
     * BR-003: Rider has surpassed 10 trips in the last year
     * BR-004: Rider gets 5% discount on trips
     */
    @Transactional(readOnly = true)
    fun checkBronzeTierEligibility(riderId: UUID): Boolean {
        val oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS)

        // Get all trips for the rider
        val allTrips = tripRepository.findByRiderIdOrderByStartedAtDesc(riderId)

        // Get trips from the last year
        val tripsInLastYear = allTrips.filter { it.startedAt.isAfter(oneYearAgo) }

        // BR-001: No missed reservations in last year (trips that were not completed)
        val missedReservationsInLastYear = tripsInLastYear.count { it.status != TripStatus.COMPLETED }
        if (missedReservationsInLastYear > 0) {
            logger.debug("Rider $riderId has $missedReservationsInLastYear missed reservations in the last year")
            return false
        }

        // BR-002: All bikes returned successfully (LIFETIME - check ALL trips ever)
        val lifetimeIncompleteTrips = allTrips.count { it.status != TripStatus.COMPLETED }
        if (lifetimeIncompleteTrips > 0) {
            logger.debug("Rider $riderId has $lifetimeIncompleteTrips incomplete trips in their lifetime")
            return false
        }

        // BR-003: At least 10 completed trips in the last year
        val completedTripsInLastYear = tripsInLastYear.count { it.status == TripStatus.COMPLETED }
        if (completedTripsInLastYear < 10) {
            logger.debug("Rider $riderId has only $completedTripsInLastYear completed trips in the last year (needs 10)")
            return false
        }

        logger.info("Rider $riderId qualifies for Bronze tier with $completedTripsInLastYear trips")
        return true
    }

    /**
     * Check if a rider qualifies for Silver tier:
     * SL-001: Rider covers Bronze tier eligibility
     * SL-002: Rider has to have at least 5 reservations of bikes that were successfully claimed within the last year
     * SL-003: Rider has surpassed 5 trips per month for the last three months
     * SL-004: Rider gets a 10% discount on trips and an extra 2-minute reservation hold
     */
    @Transactional(readOnly = true)
    fun checkSilverTierEligibility(riderId: UUID): Boolean {
        // SL-001: Must have Bronze tier eligibility
        if (!checkBronzeTierEligibility(riderId)) {
            return false
        }

        val oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS)
        val allTrips = tripRepository.findByRiderIdOrderByStartedAtDesc(riderId)
        val completedTripsLastYear = allTrips.filter {
            it.startedAt.isAfter(oneYearAgo) && it.status == TripStatus.COMPLETED
        }

        // SL-002: At least 5 reservations successfully claimed (completed trips count as successful claims)
        if (completedTripsLastYear.size < 5) {
            logger.debug("Rider $riderId has only ${completedTripsLastYear.size} successfully claimed reservations in last year (needs 5)")
            return false
        }

        // SL-003: At least 5 trips per month for last 3 months
        val now = Instant.now()
        for (monthOffset in 0..2) {
            val monthStart = now.minus((monthOffset + 1) * 30L, ChronoUnit.DAYS)
            val monthEnd = now.minus(monthOffset * 30L, ChronoUnit.DAYS)

            val tripsInMonth = allTrips.count { trip ->
                trip.startedAt.isAfter(monthStart) &&
                trip.startedAt.isBefore(monthEnd) &&
                trip.status == TripStatus.COMPLETED
            }

            if (tripsInMonth < 5) {
                logger.debug("Rider $riderId has only $tripsInMonth trips in month $monthOffset (needs 5)")
                return false
            }
        }

        logger.info("Rider $riderId qualifies for Silver tier")
        return true
    }

    /**
     * Check if a rider qualifies for Gold tier:
     * GL-001: Rider covers Silver tier eligibility
     * GL-002: Rider surpasses 5 trips every week for the last 3 months
     * GL-003: Rider gets a 15% discount on trips and an extra 5-minute reservation hold
     */
    @Transactional(readOnly = true)
    fun checkGoldTierEligibility(riderId: UUID): Boolean {
        // GL-001: Must have Silver tier eligibility
        if (!checkSilverTierEligibility(riderId)) {
            return false
        }

        val allTrips = tripRepository.findByRiderIdOrderByStartedAtDesc(riderId)
        val now = Instant.now()

        // GL-002: At least 5 trips every week for last 3 months (12 weeks)
        for (weekOffset in 0..11) {
            val weekStart = now.minus((weekOffset + 1) * 7L, ChronoUnit.DAYS)
            val weekEnd = now.minus(weekOffset * 7L, ChronoUnit.DAYS)

            val tripsInWeek = allTrips.count { trip ->
                trip.startedAt.isAfter(weekStart) &&
                trip.startedAt.isBefore(weekEnd) &&
                trip.status == TripStatus.COMPLETED
            }

            if (tripsInWeek < 5) {
                logger.debug("Rider $riderId has only $tripsInWeek trips in week $weekOffset (needs 5)")
                return false
            }
        }

        logger.info("Rider $riderId qualifies for Gold tier")
        return true
    }

    /**
     * Calculate and update the loyalty tier for a user based on all criteria
     * If a rider fails to meet criteria for their current tier, they get downgraded
     */
    @Transactional
    fun updateLoyaltyTier(riderId: UUID): LoyaltyTier {
        val user = userRepository.findById(riderId).orElseThrow {
            throw NoSuchElementException("User not found: $riderId")
        }

        val oldTier = user.loyaltyTier

        // Check from highest to lowest tier
        val newTier = when {
            checkGoldTierEligibility(riderId) -> LoyaltyTier.GOLD
            checkSilverTierEligibility(riderId) -> LoyaltyTier.SILVER
            checkBronzeTierEligibility(riderId) -> LoyaltyTier.BRONZE
            else -> LoyaltyTier.NONE
        }

        // Only update if tier changed
        if (oldTier != newTier) {
            user.loyaltyTier = newTier
            userRepository.save(user)
            logger.info("Updated loyalty tier for rider $riderId from $oldTier to $newTier")
        }

        return newTier
    }

    /**
     * Get the discount multiplier for a user based on their loyalty tier
     */
    fun getDiscountMultiplier(user: User): Float {
        return 1f - user.loyaltyTier.discountPercentage
    }

    /**
     * Apply loyalty discount to a cost
     */
    fun applyDiscount(cost: Float, user: User): Float {
        return cost * getDiscountMultiplier(user)
    }

    /**
     * Get the total number of completed trips for a user
     */
    @Transactional(readOnly = true)
    fun getCompletedTripsCount(riderId: UUID): Int {
        val allTrips = tripRepository.findByRiderIdOrderByStartedAtDesc(riderId)
        return allTrips.count { it.status == TripStatus.COMPLETED }
    }

    /**
     * Get the reservation hold time in minutes for a user based on their tier
     */
    fun getReservationHoldMinutes(user: User): Int {
        return 15 + user.loyaltyTier.reservationHoldExtraMinutes
    }

    /**
     * Get detailed loyalty progress information for a user
     */
    @Transactional(readOnly = true)
    fun getLoyaltyProgress(riderId: UUID): LoyaltyProgressDTO {
        val user = userRepository.findById(riderId).orElseThrow {
            throw NoSuchElementException("User not found: $riderId")
        }

        val oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS)
        val allTrips = tripRepository.findByRiderIdOrderByStartedAtDesc(riderId)

        // Count trips in the last year
        val tripsInLastYear = allTrips.filter { it.startedAt.isAfter(oneYearAgo) }
        val completedTripsInLastYear = tripsInLastYear.count { it.status == TripStatus.COMPLETED }
        val incompleteTripsInLastYear = tripsInLastYear.count { it.status != TripStatus.COMPLETED }

        // Determine next tier and requirements
        val currentTier = user.loyaltyTier

        return when (currentTier) {
            LoyaltyTier.NONE -> {
                LoyaltyProgressDTO(
                    currentTier = currentTier.name,
                    currentTierDisplayName = currentTier.displayName,
                    currentDiscount = (currentTier.discountPercentage * 100).toInt(),
                    nextTier = LoyaltyTier.BRONZE.name,
                    nextTierDisplayName = LoyaltyTier.BRONZE.displayName,
                    nextTierDiscount = (LoyaltyTier.BRONZE.discountPercentage * 100).toInt(),
                    requirementsForNext = mapOf(
                        "completedTrips" to completedTripsInLastYear.toString(),
                        "requiredTrips" to "10",
                        "noIncompleteTrips" to if (incompleteTripsInLastYear == 0) "true" else "false",
                        "timeframe" to "last year"
                    ),
                    currentProgress = mapOf(
                        "completedTrips" to completedTripsInLastYear.toString(),
                        "incompleteTrips" to incompleteTripsInLastYear.toString(),
                        "meetsNoIncompleteRequirement" to if (incompleteTripsInLastYear == 0) "true" else "false"
                    ),
                    totalCompletedTrips = allTrips.count { it.status == TripStatus.COMPLETED }
                )
            }
            LoyaltyTier.BRONZE -> {
                val now = Instant.now()
                val monthlyTrips = mutableListOf<Int>()
                for (monthOffset in 0..2) {
                    val monthStart = now.minus((monthOffset + 1) * 30L, ChronoUnit.DAYS)
                    val monthEnd = now.minus(monthOffset * 30L, ChronoUnit.DAYS)
                    val tripsInMonth = allTrips.count { trip ->
                        trip.startedAt.isAfter(monthStart) &&
                        trip.startedAt.isBefore(monthEnd) &&
                        trip.status == TripStatus.COMPLETED
                    }
                    monthlyTrips.add(tripsInMonth)
                }

                LoyaltyProgressDTO(
                    currentTier = currentTier.name,
                    currentTierDisplayName = currentTier.displayName,
                    currentDiscount = (currentTier.discountPercentage * 100).toInt(),
                    nextTier = LoyaltyTier.SILVER.name,
                    nextTierDisplayName = LoyaltyTier.SILVER.displayName,
                    nextTierDiscount = (LoyaltyTier.SILVER.discountPercentage * 100).toInt(),
                    requirementsForNext = mapOf(
                        "minTripsPerMonth" to "5",
                        "monthsRequired" to "3",
                        "timeframe" to "last 3 months"
                    ),
                    currentProgress = mapOf(
                        "completedTrips" to completedTripsInLastYear.toString(),
                        "incompleteTrips" to incompleteTripsInLastYear.toString(),
                        "monthlyTrips" to monthlyTrips.joinToString(","),
                        "monthsMeetingRequirement" to monthlyTrips.count { it >= 5 }.toString()
                    ),
                    totalCompletedTrips = allTrips.count { it.status == TripStatus.COMPLETED }
                )
            }
            LoyaltyTier.SILVER -> {
                val now = Instant.now()
                val weeklyTrips = mutableListOf<Int>()
                for (weekOffset in 0..11) {
                    val weekStart = now.minus((weekOffset + 1) * 7L, ChronoUnit.DAYS)
                    val weekEnd = now.minus(weekOffset * 7L, ChronoUnit.DAYS)

                    val tripsInWeek = allTrips.count { trip ->
                        trip.startedAt.isAfter(weekStart) &&
                        trip.startedAt.isBefore(weekEnd) &&
                        trip.status == TripStatus.COMPLETED
                    }

                    weeklyTrips.add(tripsInWeek)
                }

                LoyaltyProgressDTO(
                    currentTier = currentTier.name,
                    currentTierDisplayName = currentTier.displayName,
                    currentDiscount = (currentTier.discountPercentage * 100).toInt(),
                    nextTier = LoyaltyTier.GOLD.name,
                    nextTierDisplayName = LoyaltyTier.GOLD.displayName,
                    nextTierDiscount = (LoyaltyTier.GOLD.discountPercentage * 100).toInt(),
                    requirementsForNext = mapOf(
                        "minTripsPerWeek" to "5",
                        "weeksRequired" to "12",
                        "timeframe" to "last 12 weeks"
                    ),
                    currentProgress = mapOf(
                        "completedTrips" to completedTripsInLastYear.toString(),
                        "weeklyTrips" to weeklyTrips.joinToString(","),
                        "weeksMeetingRequirement" to weeklyTrips.count { it >= 5 }.toString()
                    ),
                    totalCompletedTrips = allTrips.count { it.status == TripStatus.COMPLETED }
                )
            }
            LoyaltyTier.GOLD -> {
                LoyaltyProgressDTO(
                    currentTier = currentTier.name,
                    currentTierDisplayName = currentTier.displayName,
                    currentDiscount = (currentTier.discountPercentage * 100).toInt(),
                    nextTier = null,
                    nextTierDisplayName = null,
                    nextTierDiscount = null,
                    requirementsForNext = emptyMap(),
                    currentProgress = mapOf(
                        "completedTrips" to completedTripsInLastYear.toString(),
                        "message" to "You've reached the highest tier!"
                    ),
                    totalCompletedTrips = allTrips.count { it.status == TripStatus.COMPLETED }
                )
            }
        }
    }
}

data class LoyaltyProgressDTO(
    val currentTier: String,
    val currentTierDisplayName: String,
    val currentDiscount: Int,
    val nextTier: String?,
    val nextTierDisplayName: String?,
    val nextTierDiscount: Int?,
    val requirementsForNext: Map<String, String>,
    val currentProgress: Map<String, String>,
    val totalCompletedTrips: Int
)
