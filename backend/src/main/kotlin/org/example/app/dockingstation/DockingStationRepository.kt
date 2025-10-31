package org.example.app.dockingstation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DockingStationRepository : JpaRepository<DockingStation, Long> {

    @Query("select d from DockingStation d where d.address.id = :addressId")
    fun findAllByAddressId(
        @Param("addressId") addressId: Long
    ): List<DockingStation>
}