package org.example.app.bmscoreandstationcontrol.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.example.app.bmscoreandstationcontrol.api.DockResponse
import org.example.app.bmscoreandstationcontrol.domain.Dock
import org.example.app.bmscoreandstationcontrol.domain.DockState
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "docks")
data class DockEntity(
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(cascade = [CascadeType.ALL])
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "bicycle_id")
    var bike: BicycleEntity? = null,

    @Column(nullable = false)
    @Convert(converter = DockStateConverter::class)
    var status: DockState = DockState.EMPTY,

    @ManyToOne
    @JoinColumn(name = "docking_station_id", nullable = true)
    var dockingStation: DockingStationEntity? = null
) {
    constructor(dock: Dock, dockingStation: DockingStationEntity? = null) : this(
        id = dock.id,
        bike = if(dock.bike == null) {null} else {BicycleEntity(dock.bike!!)},
        status = dock.status,
        dockingStation = dockingStation
    )

    fun toDomain(): Dock =
        Dock(
            id = id,
            bike = bike?.toDomain(),
            status = status
        )
    fun toResponse(): DockResponse =
        DockResponse(
            id = id.toString(),
            status = status.displayName,
            bike = bike?.toResponse()
        )
}

@Converter(autoApply = true)
class DockStateConverter : AttributeConverter<DockState, String> {
    override fun convertToDatabaseColumn(attribute: DockState?): String? {
        return attribute?.displayName
    }

    override fun convertToEntityAttribute(dbData: String?): DockState? {
        return DockState.entries.find { it.displayName == dbData }
    }
}