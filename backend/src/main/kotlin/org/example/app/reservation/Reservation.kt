package org.example.app.reservation

import jakarta.persistence.*
import org.example.app.bike.Bicycle
import org.example.app.user.User
import java.time.Instant
import java.util.UUID

@Entity
data class Reservation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Changed from rider_id to user_id
    val user: User,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id")
    val bike: Bicycle,

    var status: String = "ACTIVE",

    val createdAt: Instant = Instant.now()
)