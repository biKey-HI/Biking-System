package org.example.app.user

import jakarta.persistence.*
import org.example.app.dockingstation.DockingStation
import org.example.app.user.Province

@Entity
@Table(
    name = "addresses",
    indexes = [ Index(columnList = "city,postalCode") ],
    uniqueConstraints = [ UniqueConstraint(
        columnNames = ["line1","line2","city","province","postalCode","country"]
    ) ]
)
data class Address(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false) val line1: String,
    @Column(nullable = true)  val line2: String? = null,
    @Column(nullable = false) val city: String,
    @Column(nullable = false) val province: Province,
    @Column(nullable = false) val postalCode: String,
    @Column(nullable = false) val country: String = "CA",

    //array of user ids
    @OneToMany(mappedBy = "address")
    val users: MutableList<User> = mutableListOf(),

    //array of docking station ids
    @OneToMany(mappedBy = "address")
    val dockingStations: MutableList<DockingStation> = mutableListOf()
)
