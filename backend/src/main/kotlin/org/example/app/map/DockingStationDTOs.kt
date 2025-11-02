package org.example.app.bmscoreandstationcontrol.api

import org.example.app.bmscoreandstationcontrol.domain.DockingStation
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
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
) {
    companion object {
        fun fromDomain(ds: org.example.app.bmscoreandstationcontrol.domain.DockingStation): DockingStationResponse =
            DockingStationResponse(
                id = ds.id.toString(),
                name = ds.name,
                address = AddressResponse.fromDomain(ds.address),
                location = LatLngResponse.fromDomain(ds.location),
                status = ds.status.toString(),
                capacity = ds.capacity,
                numFreeDocks = ds.numFreeDocks,
                numOccupiedDocks = ds.numOccupiedDocks,
                aBikeIsReserved = ds.aBikeIsReserved,
                reservationHoldTime = ds.reservationHoldTime.toMinutes(),
                docks = ds.docks.map { DockResponse.fromDomain(it) },
                stateChanges = ds.stateChanges?.map { DockingStationStateTransitionResponse.fromDomain(it) } ?: emptyList()
            )
    }

    fun toDomain(): DockingStation {
        val station = DockingStation(
            id = UUID.fromString(id),
            name = name,
            address = org.example.app.user.Address(
                line1 = address.line1,
                line2 = address.line2,
                city = address.city,
                province = org.example.app.user.Province.valueOf(address.province),
                postalCode = address.postalCode,
                country = address.country
            ),
            location = org.example.app.bmscoreandstationcontrol.domain.LatLng(
                latitude = location.latitude,
                longitude = location.longitude
            ),
            status = null,
            capacity = capacity,
            numFreeDocks = numFreeDocks,
            numOccupiedDocks = numOccupiedDocks,
            aBikeIsReserved = aBikeIsReserved,
            reservationHoldTime = java.time.Duration.ofMinutes(reservationHoldTime),
            stateChanges = mutableListOf(),
            docks = docks.map { it.toDomain() }.toMutableList(),
            reservationUserId = if(reservationUserId == null) {null} else {UUID.fromString(reservationUserId)}
        )

        val restoredState = when (status) {
            "Empty" -> org.example.app.bmscoreandstationcontrol.domain.Empty(station)
            "Partially Filled" -> org.example.app.bmscoreandstationcontrol.domain.PartiallyFilled(station)
            "Full" -> org.example.app.bmscoreandstationcontrol.domain.Full(station)
            "Out of Service" -> org.example.app.bmscoreandstationcontrol.domain.OutOfService(station)
            else -> org.example.app.bmscoreandstationcontrol.domain.Empty(station)
        }
        station.status = restoredState

        station.stateChanges = stateChanges.map { it.toDomain(station) }.toMutableList()

        return station
    }
}

@Serializable
data class AddressResponse(
    val line1: String,
    val line2: String?,
    val city: String,
    val province: String,
    val postalCode: String,
    val country: String
) {
    companion object {
        fun fromDomain(address: org.example.app.user.Address) = AddressResponse(
            line1 = address.line1,
            line2 = address.line2,
            city = address.city,
            province = address.province.name,
            postalCode = address.postalCode,
            country = address.country
        )
    }
}

@Serializable
data class LatLngResponse(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        fun fromDomain(latLng: org.example.app.bmscoreandstationcontrol.domain.LatLng) =
            LatLngResponse(latLng.latitude, latLng.longitude)
    }
}

@Serializable
data class DockResponse(
    val id: String,
    val bike: BicycleResponse?,
    val status: String
) {
    companion object {
        fun fromDomain(dock: org.example.app.bmscoreandstationcontrol.domain.Dock) =
            DockResponse(
                id = dock.id.toString(),
                bike = dock.bike?.let { BicycleResponse.fromDomain(it) },
                status = dock.status.toString()
            )
    }

    fun toDomain(): org.example.app.bmscoreandstationcontrol.domain.Dock {
        return org.example.app.bmscoreandstationcontrol.domain.Dock(
            id = UUID.fromString(id),
            bike = bike?.toDomain(),
            status = org.example.app.bmscoreandstationcontrol.domain.DockState.fromString(status)
        )
    }
}

@Serializable
data class BicycleResponse(
    val id: String,
    val status: String,
    val reservationExpiryTime: String? = null,
    val statusTransitions: List<BikeStateTransitionResponse> = emptyList(),
    val isEBike: Boolean = false
) {
    companion object {
        fun fromDomain(bike: org.example.app.bmscoreandstationcontrol.domain.Bicycle) =
            BicycleResponse(
                id = bike.id.toString(),
                status = bike.status.toString(),
                reservationExpiryTime = bike.reservationExpiryTime?.toString(),
                statusTransitions = bike.statusTransitions.map { BikeStateTransitionResponse.fromDomain(it) },
                isEBike = bike is org.example.app.bmscoreandstationcontrol.domain.EBike
            )
    }

    fun toDomain(): org.example.app.bmscoreandstationcontrol.domain.Bicycle {
        val bikeStatus = org.example.app.bmscoreandstationcontrol.domain.BikeState.fromString(status)
        val transitions = statusTransitions.map { it.toDomain() }.toMutableList()
        val expiry = reservationExpiryTime?.let { java.time.Instant.parse(it) }

        return if (isEBike) {
            org.example.app.bmscoreandstationcontrol.domain.EBike(
                id = UUID.fromString(id),
                status = bikeStatus,
                statusTransitions = transitions,
                reservationExpiryTime = expiry
            )
        } else {
            org.example.app.bmscoreandstationcontrol.domain.Bike(
                id = UUID.fromString(id),
                status = bikeStatus,
                statusTransitions = transitions,
                reservationExpiryTime = expiry
            )
        }
    }
}

@Serializable
data class BikeStateTransitionResponse(
    val forBikeId: String,
    val fromState: String,
    val toState: String,
    val atTime: String
) {
    companion object {
        fun fromDomain(transition: org.example.app.bmscoreandstationcontrol.domain.BikeStateTransition) =
            BikeStateTransitionResponse(
                forBikeId = transition.forBikeId.toString(),
                fromState = transition.fromState.toString(),
                toState = transition.toState.toString(),
                atTime = transition.atTime.toString()
            )
    }

    fun toDomain(): org.example.app.bmscoreandstationcontrol.domain.BikeStateTransition {
        return org.example.app.bmscoreandstationcontrol.domain.BikeStateTransition(
            forBikeId = UUID.randomUUID(),
            fromState = org.example.app.bmscoreandstationcontrol.domain.BikeState.fromString(fromState),
            toState = org.example.app.bmscoreandstationcontrol.domain.BikeState.fromString(toState),
            atTime = java.time.Instant.parse(atTime)
        )
    }
}

@Serializable
data class DockingStationStateTransitionResponse(
    val forStationId: String,
    val fromState: String,
    val toState: String,
    val atTime: String
) {
    companion object {
        fun fromDomain(transition: org.example.app.bmscoreandstationcontrol.domain.DockingStationStateTransition) =
            DockingStationStateTransitionResponse(
                forStationId = transition.forStationId.toString(),
                fromState = transition.fromState.toString(),
                toState = transition.toState.toString(),
                atTime = transition.atTime.toString()
            )
    }

    fun toDomain(station: DockingStation): org.example.app.bmscoreandstationcontrol.domain.DockingStationStateTransition {
        val fromState = when (fromState) {
            "Empty" -> org.example.app.bmscoreandstationcontrol.domain.Empty(station)
            "Partially Filled" -> org.example.app.bmscoreandstationcontrol.domain.PartiallyFilled(station)
            "Full" -> org.example.app.bmscoreandstationcontrol.domain.Full(station)
            "Out of Service" -> org.example.app.bmscoreandstationcontrol.domain.OutOfService(station)
            else -> org.example.app.bmscoreandstationcontrol.domain.Empty(station)
        }
        val toState = when (toState) {
            "Empty" -> org.example.app.bmscoreandstationcontrol.domain.Empty(station)
            "Partially Filled" -> org.example.app.bmscoreandstationcontrol.domain.PartiallyFilled(station)
            "Full" -> org.example.app.bmscoreandstationcontrol.domain.Full(station)
            "Out of Service" -> org.example.app.bmscoreandstationcontrol.domain.OutOfService(station)
            else -> org.example.app.bmscoreandstationcontrol.domain.Empty(station)
        }
        return org.example.app.bmscoreandstationcontrol.domain.DockingStationStateTransition(
            forStationId = station.id,
            fromState = fromState,
            toState = toState,
            atTime = java.time.Instant.parse(atTime)
        )
    }
}