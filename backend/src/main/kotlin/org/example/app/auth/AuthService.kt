// Business logic for registration: Checks if email already exists; if yes,
// throws EmailAlreadyUsedException. Then it hashes the password with BCrypt and
// saves a User. Finally, it returns the created user id + email.
package org.example.app.auth

import org.example.app.user.User
import org.example.app.user.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class AuthService(
    private val userRepository: UserRepository
) {
    private val encoder = BCryptPasswordEncoder()

    fun register(req: RegisterRequest): RegisterResponse {
        if (userRepository.existsByEmail(req.email)) {
            throw EmailAlreadyUsedException()
        }
        if (userRepository.existsByUsername(req.username)) {
            throw UsernameAlreadyUsedException()
        }
        val user = userRepository.save(
            User(
                email = req.email,
                passwordHash = encoder.encode(req.password),
                firstName = req.firstName,
                lastName = req.lastName,
                username = req.username
            )
        )
        return RegisterResponse(
            id = requireNotNull(user.id),
            email = user.email
        )
    }

    @Transactional(readOnly = true)
    fun login(req: LoginRequest): LoginResponse {
        val email = req.email.trim()

        // Unwrap nullable result; throw if not found
        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException()

        // Now 'user' is non-null, so these are fine:
        if (!encoder.matches(req.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val id = user.id ?: error("Persisted user has null id")
        val token = TokenUtil.issueFakeToken(id, user.email)

        return LoginResponse(token = token, email = user.email, userId = id)
    }
}

class EmailAlreadyUsedException : RuntimeException("EMAIL_IN_USE: " +
        "This email is already used. Please use another email or login with this one.")

class UsernameAlreadyUsedException :
    RuntimeException("USERNAME_IN_USE: This username is already taken. Choose another one.")

class InvalidCredentialsException :
    RuntimeException("INVALID_CREDENTIALS: Email or password is incorrect.")