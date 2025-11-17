// JPA entity mapped to table users: id (auto-generated), email (unique),
// passwordHash, displayName, createdAt.
package org.example.app.user

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID
import org.example.app.loyalty.LoyaltyTier

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["email"]),
        UniqueConstraint(name = "ux_users_username", columnNames = ["username"])
    ]
)
class User(
    @JdbcTypeCode(SqlTypes.CHAR) @Id @GeneratedValue @Column(columnDefinition = "CHAR(36)")
    var id: UUID? = null,

    @Column(nullable = true)
    var notificationToken: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.MERGE])
    @JoinColumn(name = "address_id", nullable = false)
    val address: Address,

    // (only present if the rider provided payment info)
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val payment: Payment? = null,

    @Column(nullable = false, length = 320)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = false, length = 120)
    var firstName: String,

    @Column(nullable = false, length = 120)
    var lastName: String,

    @Column(nullable = false, length = 120)
    var username: String,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    // Role default to Rider
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole = UserRole.RIDER, // it always defaults to a rider

    @Enumerated(EnumType.STRING)
    @Column(name = "paymentStrategy", nullable = false)
    var paymentStrategy: PaymentStrategyType = PaymentStrategyType.DEFAULT_PAY_NOW,

    @Column(nullable = false)
    var hasActiveSubscription: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(name = "loyalty_tier", nullable = false)
    var loyaltyTier: LoyaltyTier = LoyaltyTier.NONE
)
