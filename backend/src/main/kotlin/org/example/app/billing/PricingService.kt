package org.example.app.billing

import org.example.app.bmscoreandstationcontrol.domain.Bicycle
import org.example.app.bmscoreandstationcontrol.domain.EBike
import org.example.app.user.PaymentStrategyType
import org.example.app.user.User
import org.springframework.stereotype.Service

@Service
class PricingService {
    fun price(bike: Bicycle, pricingPlan: PaymentStrategyType, rider: User): CostBreakdownDTO {
        val total = bike.calculateCost(pricingPlan) ?: 0f
        val flexDollars = rider.useFlexDollars(total)
        return CostBreakdownDTO(
            baseCents = (bike.baseCost*100).toInt(),
            perMinuteCents = 0,
            minutes = bike.getDuration()?.toMinutes()?.toInt() ?: 0,
            eBikeSurchargeCents = if(bike is EBike) (bike.baseRate*(bike.getDuration()?.toMinutes() ?: 0)*100).toInt() else 0,
            overtimeCents = ((bike.getOvertimeCost() ?: 0f)*100).toInt(),
            flexDollarCents = (flexDollars*100).toInt(),
            totalCents = ((total - flexDollars)*100).toInt()
        )
    }
}
