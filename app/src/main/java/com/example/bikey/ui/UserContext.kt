package com.example.bikey.ui
import androidx.navigation.NavHostController
import java.util.UUID

class User(val id: UUID = UUID.randomUUID(),
           val email: String,
           val isRider: Boolean = true,
           val isOperator: Boolean = false,
           var pricingPlan: PricingPlan? = if(!isRider) {null} else {PricingPlan.DEFAULT_PAY_NOW},
           var hasReservation: Boolean = false,
           var reservationStationId: String? = null,
           var flexDollars: Float = 0.0f)

class UserContext {
    companion object {
        var user: User? = null
        val id: UUID? get() = user?.id

        val isRider: Boolean get() = user?.isRider ?: true

        val isOperator: Boolean get() = user?.isOperator ?: false

        val email: String? get() = user?.email
        val pricingPlan: PricingPlan? get() = user?.pricingPlan
        val hasReservation: Boolean? get() = user?.hasReservation
        val reservationStationId: String? get() = user?.reservationStationId
        val flexDollars: Float get() = user?.flexDollars ?: 0.0f
        var notificationToken: String? = null
        var nav: NavHostController? = null
    }
}

enum class PricingPlan(val displayName: String) {
    DEFAULT_PAY_NOW("Pay As You Go"),
    MONTHLY_SUBSCRIPTION("Monthly Pass"),
    ANNUAL_SUBSCRIPTION("Annual Pass");

    override fun toString(): String {return displayName}

    val baseCostBike: Double get() = when (this) {DEFAULT_PAY_NOW -> 1.0; else -> 0.0}
    val baseCostEBike: Double get() = when (this) {DEFAULT_PAY_NOW -> 0.75; else -> 0.0}
    val baseRateBike: Double = 0.0
    val baseRateEBike: Double = 0.20
    val timeBikeMinutes: Int = 45
    val timeEBikeMinutes: Int = 120
    val overtimeRateBike: Double = 0.20
    val overtimeRateEBike: Double = 0.10
}