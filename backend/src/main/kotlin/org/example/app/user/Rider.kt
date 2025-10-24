package org.example.app.user

import jakarta.persistence.*

@Entity
@DiscriminatorValue("RIDER")
class Rider(
    address: Address,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
    username: String,
) : User(
    address = address,
    email = email,
    passwordHash = passwordHash,
    firstName = firstName,
    lastName = lastName,
    username = username,
    role = UserRole.RIDER
) {
    // Rider-specific relationship — reservations
    @OneToMany(mappedBy = "rider", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val reservations: MutableList<Reservation> = mutableListOf()
}
