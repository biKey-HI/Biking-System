package org.example.app.map

import org.example.app.bmscoreandstationcontrol.api.DockingStationResponse
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@RestController
@RequestMapping("/api/map")
class MapController(
    private val mapService: MapService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun map(): List<DockingStationResponse> {
        LoggerFactory.getLogger(MapController::class.java).info("Map endpoint hit!")
        return mapService.getAllDockingStations()
    }
}

@Service
class MapService(
    private val dockingStationRepository: DockingStationRepository
) {
    @Transactional(readOnly = true)
    fun getAllDockingStations(): List<DockingStationResponse> =
        dockingStationRepository.findAll()
            .map { it.toResponse() }
}