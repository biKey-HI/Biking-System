package org.example.app.reservation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReservationRepository : JpaRepository<Reservation, Long> {
    fun findByRiderIdAndStatus(riderId: Long, status: String): Reservation?
}
