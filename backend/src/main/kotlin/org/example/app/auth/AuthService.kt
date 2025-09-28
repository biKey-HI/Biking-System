// Business logic for registration: Checks if email already exists; if yes,
// throws EmailAlreadyUsedException. Then it hashes the password with BCrypt and
// saves a User. Finally, it returns the created user id + email.
package org.example.app.auth

import org.example.app.user.User
import org.example.app.user.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository
) {
    private val encoder = BCryptPasswordEncoder()

    fun register(req: RegisterRequest): RegisterResponse {
        if (userRepository.existsByEmail(req.email)) {
            throw EmailAlreadyUsedException()
        }
        val user = userRepository.save(
            User(
                email = req.email,
                passwordHash = encoder.encode(req.password),
                displayName = req.displayName
            )
        )
        return RegisterResponse(
            id = requireNotNull(user.id),
            email = user.email
        )
    }
}

class EmailAlreadyUsedException : RuntimeException("EMAIL_IN_USE")
