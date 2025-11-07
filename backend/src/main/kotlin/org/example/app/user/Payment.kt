package org.example.app.user

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [ UniqueConstraint(columnNames = ["user_id"]) ]
)
data class Payment(
    @JdbcTypeCode(SqlTypes.CHAR) @Id @GeneratedValue @Column(columnDefinition = "CHAR(36)")
    val id: UUID? = null,


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User? = null,

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

