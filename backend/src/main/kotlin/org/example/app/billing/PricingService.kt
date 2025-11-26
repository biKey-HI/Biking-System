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

        // 1. Determine total discount percentage (Loyalty + Operator)
        var discountPercentage = if (user != null) user.loyaltyTier.discountPercentage else 0f

        // additional 25% discount if the user is an operator
        if (user != null && user.isOperator) {
            discountPercentage += 0.25f
        }

        // total discount amount
        val discountAmount = baseCost * discountPercentage

        // apply Flex Dollars to the remaining amount
        val flexDollars = user?.let { it.useFlexDollars(baseCost - discountAmount) } ?: 0f

        // calculate Final Cost
        val finalCost = baseCost - discountAmount - flexDollars

        // determine display string for the tier (e.g., "Gold + Operator")
        val tierDisplayName = user?.loyaltyTier?.displayName
        val displayTier = if (user != null && user.isOperator) "$tierDisplayName + Operator" else tierDisplayName
        
        return CostBreakdownDTO(
            baseCents = (bike.baseCost*100).toInt(),
            perMinuteCents = 0,
            minutes = bike.getDuration()?.toMinutes()?.toInt() ?: 0,
            eBikeSurchargeCents = if (bike is EBike) (bike.baseRate*(bike.getDuration()
                ?.toMinutes() ?: 0)*100).toInt() else 0,
            overtimeCents = ((bike.getOvertimeCost() ?: 0f)*100).toInt(),
            discountCents = (discountAmount*100).toInt(),
            loyaltyTier = displayTier,
            totalCents = (finalCost*100).toInt(),
            flexDollarCents = (flexDollars*100).toInt()
        )
    }
}
