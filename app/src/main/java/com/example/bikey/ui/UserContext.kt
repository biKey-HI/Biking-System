package com.example.bikey.ui
import androidx.navigation.NavHostController
import com.example.bikey.ui.network.directionsApi
import com.example.bikey.ui.operator.model.DockingStationResponse
import java.util.UUID

class User(val id: UUID = UUID.randomUUID(), val email: String, val isOperator: Boolean = false, var pricingPlan: PricingPlan? = if(isOperator) {null} else {PricingPlan.DEFAULT_PAY_NOW}, var hasReservation: Boolean = false, var reservationStationId: String? = null, var flexDollars: Float = 0.0f, var kilometersTravelled: Int = 0)

class UserContext {
    companion object {
        var user: User? = null

        val id: UUID? get() = user?.id
        val isOperator: Boolean? get() = user?.isOperator
        val email: String? get() = user?.email
        var pricingPlan: PricingPlan?
            get() = user?.pricingPlan
            set(plan) {user?.pricingPlan = plan}
        var hasReservation: Boolean?
            get() = user?.hasReservation
            set(has) {user?.hasReservation = has == true}
        var reservationStationId: String?
            get() = user?.reservationStationId
            set(id) {user?.reservationStationId = id}
        var flexDollars: Float
            get() = user?.flexDollars ?: 0.0f
            set(dollars) {user?.flexDollars = dollars}
        var kilometersTravelled: Int
            get() = user?.kilometersTravelled ?: 0
            set(kilometers) {user?.kilometersTravelled = kilometers}

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