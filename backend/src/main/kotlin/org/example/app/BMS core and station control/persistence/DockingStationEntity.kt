package org.example.app.bmscoreandstationcontrol.persistence

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.example.app.bmscoreandstationcontrol.api.AddressResponse
import org.example.app.bmscoreandstationcontrol.api.DockingStationResponse
import org.example.app.bmscoreandstationcontrol.api.LatLngResponse
import org.example.app.bmscoreandstationcontrol.domain.DockingStation
import org.example.app.bmscoreandstationcontrol.domain.DockingStationState
import org.example.app.bmscoreandstationcontrol.domain.Empty
import org.example.app.bmscoreandstationcontrol.domain.Full
import org.example.app.bmscoreandstationcontrol.domain.LatLng
import org.example.app.bmscoreandstationcontrol.domain.OutOfService
import org.example.app.bmscoreandstationcontrol.domain.PartiallyFilled
import org.example.app.user.Address
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

@Entity
@Table(name = "docking_stations")
data class DockingStationEntity(
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    var address: Address,

    @Embedded
    var location: LatLng,

    @Column(nullable = false)
    var status: String = "Empty",

    @Column(nullable = false)
    var capacity: Int = 20,

    @Column(nullable = false)
    var numFreeDocks: Int = 20,

    @Column(nullable = false)
    var numOccupiedDocks: Int = 0,

    @Column(nullable = false)
    var aBikeIsReserved: Boolean = false,

    @Column(nullable = false)
    var reservationHoldTime: Long = 10,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    var stateChanges: MutableList<DockingStationStateTransitionEntity> = mutableListOf(),

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    var docks: MutableList<DockEntity> = mutableListOf(),

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    var reservationUserId: UUID? = null
) {
    constructor(ds: DockingStation) : this(
        id = ds.id,
        name = ds.name,
        address = ds.address,
        location = ds.location,
        status = ds.status.toString(),
        capacity = ds.capacity,
        numFreeDocks = ds.numFreeDocks,
        numOccupiedDocks = ds.numOccupiedDocks,
        aBikeIsReserved = ds.aBikeIsReserved,
        reservationHoldTime = ds.reservationHoldTime.toMinutes(),
        stateChanges = ds.stateChanges?.map {
            DockingStationStateTransitionEntity(
                forStationId = it.forStationId,
                fromState = it.fromState.toString(),
                toState = it.toState.toString(),
                atTime = it.atTime
            )
        }?.toMutableList() ?: mutableListOf(),
        docks = ds.docks.map { DockEntity(it) }.toMutableList(),
        reservationUserId = ds.reservationUserId
    )

    fun toDomain(): DockingStation {
        val station = DockingStation(
            id = id,
            name = name,
            address = address,
            location = location,
            status = null,
            capacity = capacity,
            numFreeDocks = numFreeDocks,
            numOccupiedDocks = numOccupiedDocks,
            aBikeIsReserved = aBikeIsReserved,
            reservationHoldTime = Duration.ofMinutes(reservationHoldTime),
            stateChanges = mutableListOf(),
            docks = docks.map { it.toDomain() }.toMutableList(),
            reservationUserId = reservationUserId
        )
        val restoredState: DockingStationState = when (status) {
            "Empty" -> Empty(station)
            "Partially Filled" -> PartiallyFilled(station)
            "Full" -> Full(station)
            "Out of Service" -> OutOfService(station)
            else -> Empty(station)
        }
        station.status = restoredState
        station.stateChanges = stateChanges.map { it.toDomain(station) }.toMutableList()
        return station
    }

    fun toResponse(): DockingStationResponse =
        DockingStationResponse(
            id = id.toString(),
            name = name,
            address = AddressResponse(
                line1 = address.line1,
                line2 = address.line2,
                city = address.city,
                province = address.province.name,
                postalCode = address.postalCode,
                country = address.country
            ),
            location = LatLngResponse(location.latitude, location.longitude),
            status = status,
            capacity = capacity,
            numFreeDocks = numFreeDocks,
            numOccupiedDocks = numOccupiedDocks,
            aBikeIsReserved = aBikeIsReserved,
            reservationHoldTime = reservationHoldTime,
            docks = docks.map { it.toResponse() },
            stateChanges = stateChanges.map { it.toResponse() },
            reservationUserId = reservationUserId?.toString()
        )
    fun offersFlexDollars(): Boolean {
        LoggerFactory.getLogger("Occupied").info("$numOccupiedDocks")
        LoggerFactory.getLogger("Free").info("$numFreeDocks")
        return numOccupiedDocks.toFloat()/(numFreeDocks + numOccupiedDocks).toFloat() < 0.25f
    }
}