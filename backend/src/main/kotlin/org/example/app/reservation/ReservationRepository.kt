package org.example.app.reservation

import org.example.app.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReservationRepository : JpaRepository<Reservation, UUID> {
    // Finds the most recent reservation for a given user with a specific status
    fun findFirstByUserAndStatusOrderByIdDesc(user: User, status: String): Reservation?
}