package com.example.bikey.ui.operator.model

data class DockingStationResponse(
    val id: String,
    val name: String,
    val address: AddressResponse,
    val location: LatLngResponse,
    val status: String,
    val capacity: Int,
    val numFreeDocks: Int,
    val numOccupiedDocks: Int,
    val aBikeIsReserved: Boolean,
    val reservationHoldTime: Long,
    val docks: List<DockResponse>,
    val stateChanges: List<DockingStationStateTransitionResponse> = emptyList(),
    val reservationUserId: String? = null
)

data class AddressResponse(
    val line1: String,
    val line2: String?,
    val city: String,
    val province: String,
    val postalCode: String,
    val country: String
)

data class LatLngResponse(
    val latitude: Double,
    val longitude: Double
)

data class DockResponse(
    val id: String,
    val bike: BicycleResponse?,
    val status: String
)

data class BicycleResponse(
    val id: String,
    val status: String,
    val reservationExpiryTime: String? = null,
    val statusTransitions: List<BikeStateTransitionResponse> = emptyList(),
    val isEBike: Boolean = false
)

data class BikeStateTransitionResponse(
    val forBikeId: String,
    val fromState: String,
    val toState: String,
    val atTime: String
)

data class DockingStationStateTransitionResponse(
    val forStationId: String,
    val fromState: String,
    val toState: String,
    val atTime: String
)