// Business logic for registration: Checks if email already exists; if yes,
// throws EmailAlreadyUsedException. Then it hashes the password with BCrypt and
// saves a User. Finally, it returns the created user id + email.
package org.example.app.auth

import org.example.app.user.User
import org.example.app.user.UserRepository
import org.example.app.user.Province
import org.example.app.user.Payment
import org.example.app.user.Address
import org.example.app.user.PaymentRepository
import org.example.app.user.AddressRepository
import org.example.app.user.PasswordResetToken
import org.example.app.user.PasswordResetTokenRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.example.app.user.UserRole
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val addressRepository: AddressRepository,
    private val paymentRepository: PaymentRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailService: EmailService
) {
    private val encoder = BCryptPasswordEncoder()

    fun sendPasswordResetLink(email:String){
        //find the email first
        val user = userRepository.findByEmail(email.trim()) ?:return  //this fails without notifying for security

        //generate reset token
        val resetToken = UUID.randomUUID().toString()
        val expiryTime = LocalDateTime.now().plusHours(1)

        //save token in cache for now
        passwordResetTokenRepository.save(
            PasswordResetToken(
                token = resetToken,
                user=user,
                expiryDate = expiryTime
            )
        )

        //send email with reset link
        val resetLink = "https://yourapp.com/reset-password?token=$resetToken"
        emailService.sendPasswordResetEmail(user.email, resetLink)

    }
    fun register(req: RegisterRequest): RegisterResponse {
        if (userRepository.existsByEmail(req.email)) {
            throw EmailAlreadyUsedException()
        }
        if (userRepository.existsByUsername(req.username)) {
            throw UsernameAlreadyUsedException()
        }
        val a = req.address
        val address = addressRepository.findByLine1AndLine2AndCityAndProvinceAndPostalCodeAndCountry(
            a.line1.trim(),
            a.line2?.trim(),
            a.city.trim(),
            a.province,
            a.postalCode.trim(),
            a.country.trim()
        ) ?: addressRepository.save(
            Address(
                line1 = a.line1.trim(),
                line2 = a.line2?.trim(),
                city = a.city.trim(),
                province = a.province,
                postalCode = a.postalCode.trim(),
                country = a.country.trim()
            )
        )
        val user = userRepository.save(
            User(
                email = req.email,
                passwordHash = encoder.encode(req.password),
                firstName = req.firstName,
                lastName = req.lastName,
                username = req.username,
                role = req.role,
                address = address
            )
        )
        // Optional: save payment if provided
        req.payment?.let { p ->
            val last4 = when {
                !p.cardLast4.isNullOrBlank() -> p.cardLast4
                !p.cardNumber.isNullOrBlank() -> p.cardNumber.filter(Char::isDigit).takeLast(4)
                else -> null
            }

            paymentRepository.save(
                Payment(
                    user = user,
                    cardHolderName = p.cardHolderName.trim(),
                    provider = p.provider,
                    token = p.token,
                    cardBrand = p.cardBrand,
                    cardLast4 = last4,
                    cardNumberEnc = null
                )
            )
        }
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