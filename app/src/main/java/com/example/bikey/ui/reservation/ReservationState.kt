package com.example.bikey.ui.reservation

import com.example.bikey.ui.bmscoreandstationcontrol.model.Bicycle
import com.example.bikey.ui.bmscoreandstationcontrol.model.DockingStation
import java.util.UUID

data class ReservationState(
    val dockingStation: DockingStation? = null,
    val availableBikes: List<Bicycle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val reservationSuccessBikeId: UUID? = null // To highlight the reserved bike
)