package org.example.app.user

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["email"])]
)
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 320)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = false, length = 120)
    var displayName: String,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
)

