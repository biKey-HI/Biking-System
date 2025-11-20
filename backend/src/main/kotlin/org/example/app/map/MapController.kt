package org.example.app.map

import org.example.app.bmscoreandstationcontrol.api.DockingStationResponse
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/map")
class MapController(private val mapService: MapService) {

    private val logger = LoggerFactory.getLogger(MapController::class.java)

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun map(): List<DockingStationResponse> {
        logger.info("Map endpoint hit!")
        return mapService.getAllDockingStations()
    }

    @GetMapping
    fun getAllStations(): List<DockingStationResponse> {
        logger.info("Get all stations endpoint hit!")
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