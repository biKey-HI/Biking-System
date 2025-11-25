package org.example.app.loyalty

enum class LoyaltyTier(
    val displayName: String,
    val discountPercentage: Float,
    val reservationHoldExtraMinutes: Int
) {
    NONE("No Tier", 0f, 0),
    BRONZE("Bronze", 0.05f, 0),      // 5% discount
    SILVER("Silver", 0.10f, 2),      // 10% discount + 2 min extra hold
    GOLD("Gold", 0.15f, 5);          // 15% discount + 5 min extra hold

    override fun toString(): String {
        return displayName
    }
}
