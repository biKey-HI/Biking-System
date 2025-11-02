package org.example.app.map

import org.example.app.bmscoreandstationcontrol.api.DockingStationResponse
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.springframework.web.bind.annotation.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@RestController
@RequestMapping("/api/map")
@CrossOrigin(origins = ["*"])
class MapController(
    private val mapService: MapService
) {
    @GetMapping
    fun getAllStations(): List<DockingStationResponse> = mapService.getAllDockingStations()
}

@Service
class MapService(
    private val dockingStationRepository: DockingStationRepository
) {
    @Transactional(readOnly = true)
    fun getAllDockingStations(): List<DockingStationResponse> =
        dockingStationRepository.findAll().map { it.toResponse() }
}