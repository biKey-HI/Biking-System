// Spring Data JPA repository with helpers: existsByEmail(email) and findByEmail(email).
package org.example.app.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
    fun findByEmail(email: String): User?
}
