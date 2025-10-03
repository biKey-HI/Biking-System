// JPA entity mapped to table users: id (auto-generated), email (unique),
// passwordHash, displayName, createdAt.
package org.example.app.user

import jakarta.persistence.*
import org.example.app.user.UserRole
import java.time.Instant

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["email"]),
        UniqueConstraint(name = "ux_users_username", columnNames = ["username"])]
)
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,


    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
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
    var role: UserRole = UserRole.RIDER // it always defaults to a rider
)

