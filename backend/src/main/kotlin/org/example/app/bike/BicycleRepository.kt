package org.example.app.bike

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BicycleRepository : JpaRepository<Bicycle, UUID>