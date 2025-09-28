package org.example.app.user

import org.springframework.data.jpa.repository.JpaRepository

open interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmail(email: String): Boolean
}
