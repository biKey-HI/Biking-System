package com.example.bikey.ui.bmscoreandstationcontrol.model

import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID
import com.google.android.gms.maps.model.LatLng
import android.location.Geocoder
import android.content.Context

val ok: Unit? = Unit
val fail = null

// -- DOCKING STATIONS -- \\

data class DockingStation(val id: UUID = UUID.randomUUID(),
    var name: String,
    var address: Address,
    var location: LatLng,
    var context: Context? = null, // No need to include unless Address is missing (useful to infer address from location)
    var status: DockingStationState? = null,
    var capacity: Int = 20,
    var numFreeDocks: Int = capacity,
    var numOccupiedDocks: Int = capacity - numFreeDocks,
    var aBikeIsReserved: Boolean = false, // Reservation truth not guaranteed when accessed directly; use updateReservation() first
    var reservationHoldTime: Duration = Duration.ofMinutes(10),
    var stateChanges: MutableList<DockingStationStateTransition>?,
    var dashboard: Dashboard = Dashboard(),
    var docks: MutableList<Dock> = MutableList(capacity) {Dock()} // Reservation truth not guaranteed when accessed directly; use updateReservation() first
    // Always include a docks list unless the station's docks are all empty
) {
    val lock = Any()
    constructor(id: UUID = UUID.randomUUID(),
    name: String? = null,
    address: Address? = null,
    location: LatLng? = null,
    context: Context? = null,
    status: DockingStationState? = null,
    capacity: Int? = null,
    numFreeDocks: Int? = null,
    numOccupiedDocks: Int? = null,
    aBikeIsReserved: Boolean = false,
    reservationHoldTime: Duration = Duration.ofMinutes(10),
    stateChanges: MutableList<DockingStationStateTransition>? = null,
    dashboard: Dashboard = Dashboard(),
    docks: MutableList<Dock>? = null
    ) : this(id = id,
        name = name ?: "",
        address = address ?: Address(number = 0, street = "", postalCode = ""),
        location = location ?: LatLng(0.0,0.0),
        context = context,
        status = null,
        capacity = capacity ?: 20,
        numFreeDocks = numFreeDocks ?: 0,
        numOccupiedDocks = numOccupiedDocks ?: 0,
        aBikeIsReserved = aBikeIsReserved,
        reservationHoldTime = reservationHoldTime,
        stateChanges = stateChanges,
        dashboard = dashboard,
        docks = docks ?: MutableList((capacity ?: 20)) {Dock()}
    ) {
        require(reservationHoldTime > Duration.ZERO) {"A docking station needs to have a reservation hold time."}

        this.capacity = when {
            capacity == null && docks == null && numFreeDocks == null && numOccupiedDocks == null -> 20
            capacity == null && docks != null -> docks.count()
            capacity == null && numFreeDocks != null && numOccupiedDocks != null -> numFreeDocks + numOccupiedDocks
            capacity == null && numFreeDocks == null && numOccupiedDocks != null -> if (20 > numOccupiedDocks) 20 else numOccupiedDocks
            capacity == null && numFreeDocks != null && numOccupiedDocks == null -> if (20 > numFreeDocks) 20 else numFreeDocks
            else -> capacity ?: 20
        }

        if(numFreeDocks == null && numOccupiedDocks != null) {
            this.numOccupiedDocks = numOccupiedDocks
            this.numFreeDocks = this.capacity - this.numOccupiedDocks
        } else if(numFreeDocks != null && numOccupiedDocks == null) {
            this.numFreeDocks = numFreeDocks
            this.numOccupiedDocks = this.capacity - this.numFreeDocks
        } else if(numFreeDocks == null && numOccupiedDocks == null) {
            if(docks != null) this.numFreeDocks = docks.filter {it.bike == null}.count()
            else this.numFreeDocks = this.capacity - (if(aBikeIsReserved) 1 else 0)
            this.numOccupiedDocks = this.capacity - this.numFreeDocks
        } else {
            this.numFreeDocks = numFreeDocks!!
            this.numOccupiedDocks = numOccupiedDocks!!
        }

        this.aBikeIsReserved = aBikeIsReserved

        require(this.capacity > 1 && this.numOccupiedDocks >= 0 && this.numFreeDocks >= 0) {"A docking station needs to have at least two docks."}
        require(this.capacity == this.numFreeDocks + this.numOccupiedDocks) {"A docking station cannot have fewer docks than capacity."}
        require(if(this.numOccupiedDocks == 0) !this.aBikeIsReserved else true) {"A docking station's bike cannot be reserved if there are no occupied docks."}
        require(if(this.aBikeIsReserved) this.docks.filter {it.bike?.status == BikeState.RESERVED && it.status==DockState.OCCUPIED}.count() == 1 else true) {"A docking station's bike cannot be reserved if its dock is out of service."}

        if(docks == null) this.docks = MutableList(this.capacity) {Dock()}

        require(this.docks.count() == this.capacity && this.docks.filter {it.status == DockState.OCCUPIED}.count() <= this.numOccupiedDocks && this.docks.filter {it.status == DockState.EMPTY}.count() <= this.numFreeDocks && this.docks.filter {it.status == DockState.OUT_OF_SERVICE}.count() == this.capacity - this.docks.filter {it.status == DockState.OCCUPIED}.count() - this.docks.filter {it.status == DockState.EMPTY}.count()) {"A docking station cannot have a count mismatch."}

        val reservedCount = this.docks.filter {it.bike?.status == BikeState.RESERVED}.count()
        require(if(this.aBikeIsReserved) reservedCount == 1 else reservedCount == 0) {"A docking station with a reservation must have exactly one reserved bike; otherwise, it must have none."}

        if(reservedCount > 0) {
            val reservedBike = this.docks.filter {it.bike?.status == BikeState.RESERVED}.first().bike
            require(reservedBike?.reservationExpiryTime != null) {"A docking station with a reservation must have a bike with reservation expiry time."}
        }

        if (status == null) {
            if(stateChanges != null && stateChanges.count() > 0) {
                this.status = stateChanges.last().toState
            } else {
                if (this.docks.filter {it.status == DockState.OUT_OF_SERVICE}.count() == this.capacity) {
                    this.status = OutOfService(this)
                } else if (this.docks.filter {it.status == DockState.EMPTY || it.status == DockState.OUT_OF_SERVICE}.count() == this.capacity) {
                    this.status = Empty(this)
                } else if (this.docks.filter {it.status == DockState.OCCUPIED || it.status == DockState.OUT_OF_SERVICE}.count() == this.capacity) {
                    this.status = Full(this)
                } else {
                    this.status = PartiallyFilled(this)
                }
            }
        }

        require(when {
            this.status is OutOfService -> (this.docks.filter {it.status == DockState.OUT_OF_SERVICE}.count() == this.capacity)
            this.status is Empty -> (this.docks.filter {it.status == DockState.EMPTY || it.status == DockState.OUT_OF_SERVICE}.count() == this.capacity)
            this.status is Full -> (this.docks.filter {it.status == DockState.OCCUPIED || it.status == DockState.OUT_OF_SERVICE}.count() == this.capacity)
            else -> (this.docks.filter {it.status == DockState.OCCUPIED}.count() > 0 && this.docks.filter {it.status == DockState.EMPTY}.count() > 0)
        } ) {"This docking station does not meet the necessary conditions to be considered ${this.status.toString().lowercase()}"}

        require(this.docks.filter {(it.bike != null && (it.status == DockState.EMPTY)) || (it.bike == null && (it.status == DockState.OCCUPIED))}.count() < 1) {"A docking station is empty only if there is no bike and is occupied only if there is a bike docked on it."}
        require(this.docks.filter {it.bike != null && (it.bike!!.status == BikeState.ON_TRIP)}.count() < 1) {"A docking station cannot hold a bike that is on a trip."}
        require(this.numOccupiedDocks == this.docks.count {it.bike != null} && this.numFreeDocks == this.docks.count {it.bike == null}) {"A docking station must have the same number of occupied docks as bikes."}

        if(stateChanges == null || stateChanges.count() < 1) this.stateChanges = mutableListOf(DockingStationStateTransition(this.id, this.status!!, this.status!!, Instant.now()))
        require(this.stateChanges!!.count() > 0) {"A docking station must have at least one state change."}
        require(this.stateChanges!!.last().toState == this.status) {"A docking station's last state change must be the current status."}

        require(address != null || (location != null && context != null)) {"A docking station must have a geographical position."}
        if(address == null && location != null && context != null) this.address = Address.fromGeocoder(context!!, location)
        else if(location == null && address != null) {
            val loc = Geocoder(context!!, Locale.CANADA).getFromLocationName("${address.number} ${address.street}, ${address.postalCode}, ${address.city}, ${address.provinceCode}", 1)
            if(!loc.isNullOrEmpty()) {this.location = LatLng(loc[0].latitude, loc[0].longitude)}
        }
        if(name == "") this.name = "${this.address.number} ${this.address.street}"
        if(context != null) this.context = context
    }
    // Below: null values indicate failure: it does not make sense to call it
    fun bikeIsAvailable(): Boolean? = status?.bikeIsAvailable()
    fun takeBike(bike: Bicycle, fromReservation: Boolean = false): Unit? = status?.takeBike(bike, fromReservation)
    fun returnBike(bike: Bicycle, dockId: UUID? = null): Unit? = status?.returnBike(bike, dockId)
    fun changeStationStatus(newStatus: DockingStationState): Unit? = status?.changeStationStatus(newStatus)
    fun reserveBike(bike: Bicycle?): Unit? = status?.reserveBike(bike)

    fun updateReservation(): Unit? {
        synchronized(lock) {
            if (aBikeIsReserved) {
                val reservedDock = docks.filter { it.bike?.status == BikeState.RESERVED }.firstOrNull()
                if (reservedDock != null && Instant.now()
                        .isAfter(reservedDock.bike!!.reservationExpiryTime!!)
                ) {
                    reservedDock.bike!!.reservationExpiryTime = null
                    reservedDock.bike!!.statusTransitions.add(
                        BikeStateTransition(
                            reservedDock.bike!!.id,
                            reservedDock.bike!!.status,
                            BikeState.AVAILABLE,
                            Instant.now()
                        )
                    )
                    reservedDock.bike!!.status = BikeState.AVAILABLE
                    aBikeIsReserved = false
                    return ok
                }
            }
        }
        return fail
    }

    companion object { // Only called by operators, not cyclists
        fun moveBike(bike: Bicycle, fromStation: DockingStation, toStation: DockingStation, toDockId: UUID? = null): Boolean? { // 3-level return indicators:
            val (first, second) = if(fromStation.id < toStation.id) fromStation to toStation else toStation to fromStation // Little trick to prevent deadlocks
            synchronized(first.lock) {
                synchronized(second.lock) {
                    if (fromStation.takeBike(bike) == null) return fail // return null if fails immediately -- bike still in station
                    return toStation.returnBike(bike, toDockId) != fail // return false if fails later -- bike out of both stations
                } // return true if does not fail -- bike was successfully placed into station
            }
        }
    }
}


// -- STATUSES -- \\

interface DockingStationState {
    val station: DockingStation
    fun setStation(station: DockingStation): Unit?
    fun changeStationStatus(status: DockingStationState): Unit?

    fun bikeIsAvailable(): Boolean?

    fun takeBike(bike: Bicycle, fromReservation: Boolean? = false): Unit?
    fun returnBike(bike: Bicycle, dockId: UUID? = null): Unit?
    fun reserveBike(bike: Bicycle? = null): Unit?
}

class Empty(override val station: DockingStation): DockingStationState {
    override fun toString(): String = "Empty"

    override fun setStation(station: DockingStation): Unit? {
        synchronized(station.lock) {
            if (station.status is Full || station.status is OutOfService || station.status is PartiallyFilled) {
                return fail
            }
            station.status = this
        }
        return ok
    }
    override fun changeStationStatus(status: DockingStationState): Unit? {
        synchronized(station.lock) {
            if (station.docks.filter { it.status == DockState.OCCUPIED }.count() < 1) {
                return fail
            }
            station.status = status
            station.stateChanges!!.add(
                DockingStationStateTransition(
                    station.id,
                    this,
                    status,
                    Instant.now()
                )
            )
        }
        return ok
    }

    override fun bikeIsAvailable(): Boolean? = false

    override fun reserveBike(bike: Bicycle?): Unit? = fail
    override fun takeBike(bike: Bicycle, fromReservation: Boolean?): Unit? = fail
    override fun returnBike(bike: Bicycle, dockId: UUID?): Unit? {
        synchronized(station.lock) {
            if (bike.status != BikeState.ON_TRIP) return fail
            if (dockId != null && station.docks.firstOrNull { it.id == dockId } == null) return fail
            if (dockId == null && station.docks.firstOrNull { it.status == DockState.EMPTY } == null) return fail
            var dock: Dock
            if (dockId != null) dock = station.docks.firstOrNull { it.id == dockId }!!
            else dock = station.docks.firstOrNull { it.status == DockState.EMPTY }!!

            dock.bike = bike

            station.numFreeDocks--
            station.numOccupiedDocks++

            bike.status = BikeState.AVAILABLE
            bike.statusTransitions.add(
                BikeStateTransition(
                    bike.id,
                    BikeState.ON_TRIP,
                    BikeState.AVAILABLE,
                    Instant.now()
                )
            )

            dock.status = DockState.OCCUPIED

            station.changeStationStatus(PartiallyFilled(station))
            station.changeStationStatus(Full(station))
        }
        return ok
    }
}

class PartiallyFilled(override val station: DockingStation): DockingStationState {
    override fun toString(): String = "Partially Filled"

    override fun setStation(station: DockingStation): Unit? {
        synchronized(station.lock) {
            if (station.status is Full || station.status is OutOfService || station.status is Empty) {
                return fail
            }
            station.status = this
        }
        return ok
    }

    override fun changeStationStatus(status: DockingStationState): Unit? {
        synchronized(station.lock) {
            if (station.docks.filter { it.status == DockState.OCCUPIED }
                    .count() > 0 && station.docks.filter { it.status == DockState.EMPTY }
                    .count() > 0) {
                return fail
            }
            station.stateChanges!!.add(
                DockingStationStateTransition(
                    station.id,
                    this,
                    status,
                    Instant.now()
                )
            )
            station.status = status
        }
        return ok
    }

    override fun bikeIsAvailable(): Boolean? {
        synchronized(station.lock) {
            station.updateReservation()
            return station.docks.filter { it.bike?.status == BikeState.AVAILABLE && it.status == DockState.OCCUPIED }
                .count() > 0
        }
    }

    override fun reserveBike(bike: Bicycle?): Unit? {
        synchronized(station.lock) {
            station.updateReservation()
            if (station.aBikeIsReserved) return fail

            if (bike != null) {
                if (station.docks.filter { it.bike?.id == bike.id }
                        .count() < 1 || station.docks.filter { it.bike?.id == bike.id && it.bike!!.status != BikeState.AVAILABLE }
                        .count() > 0) return fail
                station.aBikeIsReserved = true
                val d = station.docks.filter { it.bike?.id == bike.id }.first()
                d.bike!!.reservationExpiryTime = Instant.now().plus(station.reservationHoldTime)
                d.bike!!.statusTransitions.add(
                    BikeStateTransition(
                        bike.id,
                        bike.status,
                        BikeState.RESERVED,
                        Instant.now()
                    )
                )
                d.bike!!.status = BikeState.RESERVED
            } else {
                if (station.docks.filter { it.bike?.status == BikeState.AVAILABLE }
                        .count() < 1) return fail
                station.aBikeIsReserved = true
                val d = station.docks.filter { it.bike?.status == BikeState.AVAILABLE }.first()
                d.bike!!.reservationExpiryTime = Instant.now().plus(station.reservationHoldTime)
                d.bike!!.statusTransitions.add(
                    BikeStateTransition(
                        d.bike!!.id,
                        d.bike!!.status,
                        BikeState.RESERVED,
                        Instant.now()
                    )
                )
                d.bike!!.status = BikeState.RESERVED
            }
        }
        return ok
    }

    override fun takeBike(bike: Bicycle, fromReservation: Boolean?): Unit? {
        val fromReservation = fromReservation ?: false

        synchronized(station.lock) {
            station.updateReservation()

            if (fromReservation) {
                if (station.docks.filter { it.bike?.id == bike.id && it.bike?.status == BikeState.RESERVED }
                        .count() < 1) return fail
                station.aBikeIsReserved = false

                val d = station.docks.filter { it.bike?.id == bike.id }.first()
                d.bike!!.reservationExpiryTime = null
                d.bike!!.statusTransitions.add(
                    BikeStateTransition(
                        bike.id,
                        BikeState.RESERVED,
                        BikeState.ON_TRIP,
                        Instant.now()
                    )
                )
                d.bike!!.status = BikeState.ON_TRIP

                d.status = DockState.EMPTY
            } else {
                if (station.docks.filter { it.bike?.id == bike.id && it.bike?.status == BikeState.AVAILABLE }
                        .count() < 1) return fail

                val d = station.docks.filter { it.bike?.id == bike.id }.first()
                d.bike!!.reservationExpiryTime = null
                d.bike!!.statusTransitions.add(
                    BikeStateTransition(
                        bike.id,
                        BikeState.AVAILABLE,
                        BikeState.ON_TRIP,
                        Instant.now()
                    )
                )
                d.bike!!.status = BikeState.ON_TRIP

                d.status = DockState.EMPTY
            }

            station.numFreeDocks++
            station.numOccupiedDocks--
            station.changeStationStatus(Empty(station))
        }
        return ok
    }

    override fun returnBike(bike: Bicycle, dockId: UUID?): Unit? {
        synchronized(station.lock) {
            station.updateReservation()

            if (bike.status != BikeState.RESERVED && station.docks.filter { it.bike?.id == bike?.id }
                    .count() > 0) return fail
            if (bike.status == BikeState.RESERVED && station.docks.filter { it.bike?.id == bike?.id && it.bike!!.status == BikeState.RESERVED }
                    .count() < 1) return fail
            if (bike.status == BikeState.ON_TRIP && station.docks.filter { it.status == DockState.EMPTY }
                    .count() < 1) return fail
            if (bike.status != BikeState.ON_TRIP && bike.status != BikeState.RESERVED) return fail

            if (bike.status == BikeState.RESERVED) {
                station.docks.filter { it.bike?.status == BikeState.RESERVED }
                    .first().bike!!.statusTransitions.add(
                    BikeStateTransition(
                        bike.id,
                        BikeState.RESERVED,
                        BikeState.AVAILABLE,
                        Instant.now()
                    )
                )
                station.docks.filter { it.bike?.status == BikeState.RESERVED }
                    .first().bike!!.status = BikeState.AVAILABLE
                station.aBikeIsReserved = false
            } else {
                val dockId = dockId ?: station.docks.filter { it.status == DockState.EMPTY }
                    .firstOrNull()?.id

                if (dockId == null) return fail
                if (station.docks.filter { it.id == dockId }
                        .count() < 1 || station.docks.filter { it.id == dockId }
                        .first().status != DockState.EMPTY) return fail

                station.docks.first { it.id == dockId }.bike = bike

                station.numFreeDocks--
                station.numOccupiedDocks++

                bike.status = BikeState.AVAILABLE
                bike.statusTransitions.add(
                    BikeStateTransition(
                        bike.id,
                        BikeState.ON_TRIP,
                        BikeState.AVAILABLE,
                        Instant.now()
                    )
                )

                station.docks.first { it.id == dockId }.status = DockState.OCCUPIED

                station.changeStationStatus(Full(station))
            }
        }
        return ok
    }
}

class Full(override val station: DockingStation): DockingStationState {
    override fun toString(): String = "Full"

    override fun setStation(station: DockingStation): Unit? {
        synchronized(station.lock) {
            if (station.status is PartiallyFilled || station.status is OutOfService || station.status is Empty) {
                return fail
            }
            station.status = this
        }
        return ok
    }

    override fun changeStationStatus(status: DockingStationState): Unit? {
        synchronized(station.lock) {
            if (station.docks.filter { it.status == DockState.EMPTY }.count() > 0) {
                return fail
            }
            station.stateChanges!!.add(
                DockingStationStateTransition(
                    station.id,
                    this,
                    status,
                    Instant.now()
                )
            )
            station.status = status
        }
        return ok
    }

    override fun bikeIsAvailable(): Boolean? {
        synchronized(station.lock) {
            station.updateReservation()
            return station.docks.filter { it.bike?.status == BikeState.AVAILABLE && it.status == DockState.OCCUPIED }
                .count() > 0
        }
    }

    override fun reserveBike(bike: Bicycle?): Unit? {
        synchronized(station.lock) {
            station.updateReservation()
            if (station.aBikeIsReserved) return null
            if (bike != null) {
                if (station.docks.filter { it.bike?.id == bike.id }
                        .count() < 1 || station.docks.filter { it.bike?.id == bike.id && it.bike!!.status != BikeState.AVAILABLE }
                        .count() > 0) return fail
                station.aBikeIsReserved = true
                val d = station.docks.filter { it.bike?.id == bike.id }.first()
                d.bike!!.reservationExpiryTime = Instant.now().plus(station.reservationHoldTime)
                d.bike!!.statusTransitions.add(
                    BikeStateTransition(
                        bike.id,
                        bike.status,
                        BikeState.RESERVED,
                        Instant.now()
                    )
                )
                d.bike!!.status = BikeState.RESERVED
            } else {
                if (station.docks.filter { it.bike?.status == BikeState.AVAILABLE }
                        .count() < 1) return fail
                station.aBikeIsReserved = true
                val d = station.docks.filter { it.bike?.status == BikeState.AVAILABLE }.first()
                d.bike!!.reservationExpiryTime = Instant.now().plus(station.reservationHoldTime)
                d.bike!!.statusTransitions.add(
                    BikeStateTransition(
                        d.bike!!.id,
                        d.bike!!.status,
                        BikeState.RESERVED,
                        Instant.now()
                    )
                )
                d.bike!!.status = BikeState.RESERVED
            }
        }
        return ok
    }

    override fun takeBike(bike: Bicycle, fromReservation: Boolean?): Unit? {
        val fromReservation = fromReservation ?: false

        synchronized(station.lock) {
            station.updateReservation()

            if (fromReservation) {
                if (station.docks.filter { it.bike?.id == bike.id && it.bike?.status == BikeState.RESERVED }
                        .count() < 1) return fail
                station.aBikeIsReserved = false

                val d = station.docks.filter { it.bike?.id == bike.id }.first()
                d.bike!!.reservationExpiryTime = null
                d.bike!!.statusTransitions.add(
                    BikeStateTransition(
                        bike.id,
                        BikeState.RESERVED,
                        BikeState.ON_TRIP,
                        Instant.now()
                    )
                )
                d.bike!!.status = BikeState.ON_TRIP

                d.status = DockState.EMPTY
            } else {
                if (station.docks.filter { it.bike?.id == bike.id && it.bike?.status == BikeState.AVAILABLE }
                        .count() < 1) return fail

                val d = station.docks.filter { it.bike?.id == bike.id }.first()
                d.bike!!.reservationExpiryTime = null
                d.bike!!.statusTransitions.add(
                    BikeStateTransition(
                        bike.id,
                        BikeState.AVAILABLE,
                        BikeState.ON_TRIP,
                        Instant.now()
                    )
                )
                d.bike!!.status = BikeState.ON_TRIP

                d.status = DockState.EMPTY
            }

            station.numFreeDocks++
            station.numOccupiedDocks--
            station.changeStationStatus(PartiallyFilled(station))
        }
        return ok
    }

    override fun returnBike(bike: Bicycle, dockId: UUID?): Unit? = fail
}

class OutOfService(override val station: DockingStation): DockingStationState {
    override fun setStation(station: DockingStation): Unit? {
        synchronized(station.lock) {
            if (station.status is PartiallyFilled || station.status is Full || station.status is Empty) {
                return fail
            }
            station.status = this
        }
        return ok
    }
    override fun changeStationStatus(status: DockingStationState): Unit? {
        synchronized(station.lock) {
            if (station.docks.filter { it.status == DockState.OUT_OF_SERVICE }
                    .count() >= station.docks.count() || station.stateChanges == null) {
                return fail
            }
            station.stateChanges!!.add(
                DockingStationStateTransition(
                    station.id,
                    this,
                    status,
                    Instant.now()
                )
            )
            station.status = status
        }
        return ok
    }

    override fun bikeIsAvailable(): Boolean? = fail

    override fun reserveBike(bike: Bicycle?): Unit? = fail
    override fun takeBike(bike: Bicycle, fromReservation: Boolean?): Unit? = fail
    override fun returnBike(bike: Bicycle, dockId: UUID?): Unit? = fail
}


data class DockingStationStateTransition(val forStationId: UUID,
    val fromState: DockingStationState,
    val toState: DockingStationState,
    val atTime: Instant
)


// -- DOCKS -- \\

data class Dock(val id: UUID = UUID.randomUUID(), var bike: Bicycle? = null, var status: DockState = DockState.EMPTY)

enum class DockState(val displayName: String) {
    EMPTY("Empty"),
    OCCUPIED("Occupied"),
    OUT_OF_SERVICE("Out of Service");

    override fun toString() = displayName
}


// -- ADDRESSES -- \\

data class Address(
    val number: Int,
    val street: String,
    val apartment: String? = null,
    val postalCode: String,
    val city: String = "Montreal",
    val province: String = "Quebec",
    val provinceCode: String = "QC",
    val country: String = "Canada"
) {
    constructor(address: String) : this( // You can input an address in text format
        number = address.substringBefore(" ").toInt(),
        street = address.substringAfter(" ").substringBefore(",").trim(),
        apartment = address.substringAfter("Apt", "").takeIf {it.isNotBlank()}?.trim(),
        postalCode = address.substringAfterLast(",").trim(),
        city = "Montreal",
        province = "Quebec",
        provinceCode = "QC",
        country = "Canada"
    )

    companion object {
        fun fromGeocoder(context: Context, location: LatLng): Address {
            val addrList = Geocoder(context, Locale.CANADA).getFromLocation(location.latitude, location.longitude, 1)
            if (!addrList.isNullOrEmpty()) {
                val streetParts = addrList[0].thoroughfare?.split(" ", limit = 2)
                val number = streetParts?.firstOrNull()?.toIntOrNull() ?: 0
                val street = streetParts?.drop(1)?.joinToString(" ") ?: addrList[0].thoroughfare ?: ""
                return Address(number = number, street = street, apartment = null, postalCode = addrList[0].postalCode ?: "", city = addrList[0].locality ?: "Montreal", province = addrList[0].adminArea ?: "Quebec", provinceCode = "QC", country = addrList[0].countryName ?: "Canada")
            }
            return Address(number = 0, street = "", postalCode = "")
        }
    }
}
