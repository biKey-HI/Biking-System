package org.example.app.user

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class AuthService(
    private val repo: UserRepository
) {
    private val encoder = BCryptPasswordEncoder()

    @Transactional
    fun register(req: RegisterRequest): RegisterResponse {
        val email = req.email.trim().lowercase()
        if (repo.existsByEmail(email)) throw EmailAlreadyUsedException()

        val user = User(
            email = email,
            passwordHash = encoder.encode(req.password),
            displayName = req.displayName.trim()
        )
        val saved = repo.save(user)
        return RegisterResponse(saved.id!!, saved.email, saved.displayName)
    }
}

open class EmailAlreadyUsedException : RuntimeException("Email already in use")
