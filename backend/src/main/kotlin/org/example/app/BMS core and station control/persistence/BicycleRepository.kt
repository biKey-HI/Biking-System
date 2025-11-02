package org.example.app.bmscoreandstationcontrol.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BicycleRepository : JpaRepository<BicycleEntity, UUID>