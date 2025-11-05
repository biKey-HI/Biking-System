package org.example.app.bmscoreandstationcontrol.persistence


import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TripRepository : JpaRepository<Trip, UUID> {
    fun findByRiderIdAndStatus(riderId: UUID, status: TripStatus): Trip?
    fun findByBikeIdAndStatus(bikeId: UUID, status: TripStatus): Trip?
}