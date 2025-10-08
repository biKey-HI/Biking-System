package org.example.app.user
import jakarta.persistence.*
import org.example.app.user.Province
import java.time.LocalDateTime

@Entity
@Table(name = "password_reset_tokens")
data class PasswordResetToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val token: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val expiryDate: LocalDateTime,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)