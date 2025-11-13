package org.example.app.bmscoreandstationcontrol.persistence

import jakarta.persistence.*
import org.example.app.bmscoreandstationcontrol.domain.Bicycle
import org.example.app.bmscoreandstationcontrol.domain.Bike
import org.example.app.bmscoreandstationcontrol.domain.BikeState
import org.example.app.bmscoreandstationcontrol.domain.EBike
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*
import org.example.app.bmscoreandstationcontrol.api.BicycleResponse

@Entity
@Table(name = "bikes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "bike_type")
open class BicycleEntity(
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    open val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    @Convert(converter = BikeStateConverter::class)
    open var status: BikeState = BikeState.AVAILABLE,

    @OneToMany(mappedBy = "bike", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    open var statusTransitions: MutableList<BikeStateTransitionEntity> = mutableListOf(),

    @Column(name = "reservation_expiry_time")
    open var reservationExpiryTime: Instant? = null,

    @Column(name = "is_ebike", nullable = false)
    open var isEBike: Boolean = false
) {
    constructor(bike: Bicycle) : this(
        id = bike.id,
        status = bike.status,
        statusTransitions = bike.statusTransitions.map { BikeStateTransitionEntity(it) }.toMutableList(),
        reservationExpiryTime = bike.reservationExpiryTime,
        isEBike = bike is EBike
    ) {statusTransitions.forEach {it.bike = this}}

    fun toDomain(): Bicycle {
        return if (isEBike) {
            EBike(
                id = id,
                status = status,
                statusTransitions = statusTransitions.map { it.toDomain() }.toMutableList(),
                reservationExpiryTime = reservationExpiryTime
            )
        } else {
            Bike(
                id = id,
                status = status,
                statusTransitions = statusTransitions.map { it.toDomain() }.toMutableList(),
                reservationExpiryTime = reservationExpiryTime
            )
        }
    }

    fun toResponse(): BicycleResponse =
        BicycleResponse(
            id = id.toString(),
            status = status.displayName,
            reservationExpiryTime = reservationExpiryTime?.toString(),
            statusTransitions = statusTransitions.map { it.toResponse() },
            isEBike = isEBike
        )
}

@Converter(autoApply = true)
class BikeStateConverter : AttributeConverter<BikeState, String> {
    override fun convertToDatabaseColumn(attribute: BikeState?): String? {
        return attribute?.displayName
    }

    override fun convertToEntityAttribute(dbData: String?): BikeState? {
        return BikeState.entries.find { it.displayName == dbData }
    }
}
