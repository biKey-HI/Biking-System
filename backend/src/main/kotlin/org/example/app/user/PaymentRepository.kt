package org.example.app.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PaymentRepository : JpaRepository<Payment, UUID> {

    fun findByUserId(userId: UUID): Payment?
    fun existsByUserId(userId: UUID): Boolean
    fun deleteByUserId(userId: UUID)
}
