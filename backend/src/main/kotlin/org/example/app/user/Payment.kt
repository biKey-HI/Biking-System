package org.example.app.user

import jakarta.persistence.*

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [ UniqueConstraint(columnNames = ["user_id"]) ]
)
data class Payment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(nullable = false)
    val cardHolderName: String,

    @Column(nullable = true)
    val provider: String? = null,

    @Column(nullable = true)
    val token: String? = null,

    @Column(nullable = true)
    val cardBrand: String? = null,

    @Column(nullable = true)
    val cardLast4: String? = null,


    @Column(nullable = true)
    val cardNumberEnc: String? = null
)

