package org.example.app.reservation

import jakarta.persistence.*
import org.example.app.bike.Bicycle
import org.example.app.user.Rider
import java.time.Instant
@Entity
@Table(name = "reservations")
class Reservation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    var rider: Rider,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", nullable = false)
    var bicycle: Bicycle,

    @Column(nullable = false)
    var status: String = "ACTIVE",  // ACTIVE / CANCELED / COMPLETED

    @Column(nullable = false)
    var reservedAt: Instant = Instant.now(),

    var expiresAt: Instant? = null
)
