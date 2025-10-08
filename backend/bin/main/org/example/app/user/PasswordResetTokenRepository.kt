package org.example.app.user
import org.springframework.data.jpa.repository.JpaRepository

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): PasswordResetToken?
    fun deleteByUser(user: User)
}