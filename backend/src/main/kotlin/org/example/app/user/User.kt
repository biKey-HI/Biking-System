// JPA entity mapped to table users: id (auto-generated), email (unique),
// passwordHash, displayName, createdAt.
package org.example.app.user

import jakarta.persistence.*
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
    var createdAt: Instant = Instant.now()
)

