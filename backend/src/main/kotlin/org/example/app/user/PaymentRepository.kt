package org.example.app.user

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {

    fun findByUserId(userId: Long): Payment?
    fun existsByUserId(userId: Long): Boolean
    fun deleteByUserId(userId: Long)
}
