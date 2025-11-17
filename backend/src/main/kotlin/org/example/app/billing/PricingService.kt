package org.example.app.billing

import org.example.app.bmscoreandstationcontrol.domain.Bicycle
import org.example.app.bmscoreandstationcontrol.domain.EBike
import org.example.app.user.PaymentStrategyType
import org.example.app.user.User
import org.example.app.loyalty.LoyaltyService
import org.springframework.stereotype.Service

@Service
class PricingService(private val loyaltyService: LoyaltyService) {
    fun price(bike: Bicycle, pricingPlan: PaymentStrategyType, user: User? = null): CostBreakdownDTO {
        val baseCost = (bike.calculateCost(pricingPlan) ?: 0f)

        // Calculate loyalty discount
        val discountAmount = if (user != null) {
            baseCost * user.loyaltyTier.discountPercentage
        } else {
            0f
        }

        val finalCost = baseCost - discountAmount

        return CostBreakdownDTO(
            baseCents = (bike.baseCost*100).toInt(),
            perMinuteCents = 0,
            minutes = bike.getDuration()?.toMinutes()?.toInt() ?: 0,
            eBikeSurchargeCents = if(bike is EBike) (bike.baseRate*(bike.getDuration()?.toMinutes() ?: 0)*100).toInt() else 0,
            overtimeCents = ((bike.getOvertimeCost() ?: 0f)*100).toInt(),
            discountCents = (discountAmount * 100).toInt(),
            loyaltyTier = user?.loyaltyTier?.displayName,
            totalCents = (finalCost*100).toInt()
        )
    }
}
