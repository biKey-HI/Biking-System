package org.example.app.loyalty

import org.example.app.user.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/loyalty")
class LoyaltyController(
    private val loyaltyService: LoyaltyService,
    private val userRepository: UserRepository
) {

    /**
     * Get the current loyalty tier for a user
     */
    @GetMapping("/tier/{userId}")
    fun getLoyaltyTier(@PathVariable userId: UUID): ResponseEntity<LoyaltyTierResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            throw NoSuchElementException("User not found: $userId")
        }

        // Get total completed trips for the user
        val completedTrips = loyaltyService.getCompletedTripsCount(userId)

        return ResponseEntity.ok(
            LoyaltyTierResponse(
                userId = userId,
                tier = user.loyaltyTier.name,
                tierDisplayName = user.loyaltyTier.displayName,
                discountPercentage = user.loyaltyTier.discountPercentage,
                totalRides = completedTrips,
                reservationHoldExtraMinutes = user.loyaltyTier.reservationHoldExtraMinutes
            )
        )
    }

    /**
     * Check if a user is eligible for Bronze tier
     */
    @GetMapping("/check-bronze-eligibility/{userId}")
    fun checkBronzeEligibility(@PathVariable userId: UUID): ResponseEntity<BronzeEligibilityResponse> {
        val isEligible = loyaltyService.checkBronzeTierEligibility(userId)

        return ResponseEntity.ok(
            BronzeEligibilityResponse(
                userId = userId,
                isEligible = isEligible,
                message = if (isEligible) {
                    "Congratulations! You qualify for Bronze tier benefits."
                } else {
                    "Keep riding to unlock Bronze tier benefits!"
                }
            )
        )
    }

    /**
     * Check tier eligibility for all tiers
     */
    @GetMapping("/check-all-eligibility/{userId}")
    fun checkAllTierEligibility(@PathVariable userId: UUID): ResponseEntity<AllTierEligibilityResponse> {
        return ResponseEntity.ok(
            AllTierEligibilityResponse(
                userId = userId,
                bronzeEligible = loyaltyService.checkBronzeTierEligibility(userId),
                silverEligible = loyaltyService.checkSilverTierEligibility(userId),
                goldEligible = loyaltyService.checkGoldTierEligibility(userId)
            )
        )
    }

    /**
     * Manually trigger loyalty tier update for a user
     * Returns the old and new tier for notification purposes
     */
    @PostMapping("/update-tier/{userId}")
    fun updateLoyaltyTier(@PathVariable userId: UUID): ResponseEntity<TierUpdateResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            throw NoSuchElementException("User not found: $userId")
        }

        val oldTier = user.loyaltyTier
        val newTier = loyaltyService.updateLoyaltyTier(userId)

        // Get total completed trips for the user
        val completedTrips = loyaltyService.getCompletedTripsCount(userId)

        return ResponseEntity.ok(
            TierUpdateResponse(
                userId = userId,
                oldTier = oldTier.name,
                newTier = newTier.name,
                tierChanged = oldTier != newTier,
                upgraded = newTier.ordinal > oldTier.ordinal,
                downgraded = newTier.ordinal < oldTier.ordinal,
                currentTierInfo = LoyaltyTierResponse(
                    userId = userId,
                    tier = newTier.name,
                    tierDisplayName = newTier.displayName,
                    discountPercentage = newTier.discountPercentage,
                    totalRides = completedTrips,
                    reservationHoldExtraMinutes = newTier.reservationHoldExtraMinutes
                )
            )
        )
    }

    /**
     * Get detailed loyalty progress information for a user
     */
    @GetMapping("/progress/{userId}")
    fun getLoyaltyProgress(@PathVariable userId: UUID): ResponseEntity<LoyaltyProgressDTO> {
        val progress = loyaltyService.getLoyaltyProgress(userId)
        return ResponseEntity.ok(progress)
    }
}

data class LoyaltyTierResponse(
    val userId: UUID,
    val tier: String,
    val tierDisplayName: String,
    val discountPercentage: Float,
    val totalRides: Int,
    val reservationHoldExtraMinutes: Int
)

data class BronzeEligibilityResponse(
    val userId: UUID,
    val isEligible: Boolean,
    val message: String
)

data class AllTierEligibilityResponse(
    val userId: UUID,
    val bronzeEligible: Boolean,
    val silverEligible: Boolean,
    val goldEligible: Boolean
)

data class TierUpdateResponse(
    val userId: UUID,
    val oldTier: String,
    val newTier: String,
    val tierChanged: Boolean,
    val upgraded: Boolean,
    val downgraded: Boolean,
    val currentTierInfo: LoyaltyTierResponse
)
