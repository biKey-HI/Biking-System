package org.example.app.user

enum class PaymentStrategyType(val displayName: String) {
    DEFAULT_PAY_NOW("Pay As You Go"),
    MONTHLY_SUBSCRIPTION("Monthly Pass"),
    ANNUAL_SUBSCRIPTION("Annual Pass");

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun fromString(strategy: String): PaymentStrategyType? {
            return when (strategy) {
                DEFAULT_PAY_NOW.toString() -> DEFAULT_PAY_NOW
                MONTHLY_SUBSCRIPTION.toString() -> MONTHLY_SUBSCRIPTION
                ANNUAL_SUBSCRIPTION.toString() -> ANNUAL_SUBSCRIPTION
                else -> null
            }
        }
    }
}
