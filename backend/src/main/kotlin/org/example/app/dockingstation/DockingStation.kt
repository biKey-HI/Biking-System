package org.example.app.dockingstation

import jakarta.persistence.*
import org.example.app.user.Address

@Entity
@Table(
    name = "docking_stations",
    indexes = [ Index(columnList = "locationName, latitude, longitude") ],
    uniqueConstraints = [ UniqueConstraint(
        columnNames = ["locationName", "latitude", "longitude"]
    ) ]
)
data class DockingStation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false) val locationName: String,
    @Column(nullable = false) val latitude: Double,
    @Column(nullable = false) val longitude: Double,
    
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    val address: Address
)
