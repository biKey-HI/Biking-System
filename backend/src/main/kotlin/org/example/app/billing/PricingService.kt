package org.example.app.billing

// import org.example.app.billing.CostBreakdownDTO
import org.springframework.stereotype.Service

@Service
class PricingService {
    data class Rules(
        val baseCents: Int = 100,                 // $1.00
        val perMinuteCents: Int = 20,             // $0.20/min
        val eBikeSurchargeCentsPerRide: Int = 150,// $1.50
        val freeMinutes: Int = 0
    )
    private val rules = Rules()

    fun price(minutes: Int, isEBike: Boolean, overtimeCents: Int): CostBreakdownDTO {
        val billable = (minutes - rules.freeMinutes).coerceAtLeast(0)
        val perMinute = billable * rules.perMinuteCents
        val eBike = if (isEBike) rules.eBikeSurchargeCentsPerRide else 0
        val total = rules.baseCents + perMinute + eBike + overtimeCents
        return CostBreakdownDTO(
            baseCents = rules.baseCents,
            perMinuteCents = rules.perMinuteCents,
            minutes = minutes,
            eBikeSurchargeCents = eBike,
            overtimeCents = overtimeCents,
            totalCents = total
        )
    }
}
