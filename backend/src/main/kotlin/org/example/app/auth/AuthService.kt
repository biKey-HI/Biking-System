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
import org.example.app.loyalty.LoyaltyService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.example.app.user.UserRole
import org.springframework.transaction.annotation.Transactional


@Service
class AuthService(
    private val userRepository: UserRepository,
    private val addressRepository: AddressRepository,
    private val paymentRepository: PaymentRepository,
    private val loyaltyService: LoyaltyService
) {
    private val encoder = BCryptPasswordEncoder()

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
                // Force RIDER role for all registrations
                isRider = true,
                isOperator = false,
                address = address,
                notificationToken = req.notificationToken
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

    @Transactional
    fun login(req: LoginRequest): LoginResponse {
        try {
            val email = req.email.trim()

            // Unwrap nullable result; throw if not found
            var user = userRepository.findByEmail(email)
                ?: throw InvalidCredentialsException()

            // Now 'user' is non-null, so these are fine:
            if (!encoder.matches(req.password, user.passwordHash)) {
                throw InvalidCredentialsException()
            }

            val id = user.id ?: error("Persisted user has null id")

            // Check for loyalty tier changes (only for riders)
            val oldTier = user.loyaltyTier
            var tierChanged = false
            var tierUpgraded = false
            var tierDowngraded = false
        // Access email, role and plan within transaction to ensure they're loaded
        val userEmail = user.email
        val userRole = user.role.name
        val isRider = user.isRider
        val isOperator= user.isOperator
        val userPricingPlan = user.paymentStrategy
        val userFlexDollars = user.flexDollars

            if (user.role == UserRole.RIDER) {
                try {
                    val newTier = loyaltyService.updateLoyaltyTier(id)
                    tierChanged = oldTier != newTier
                    tierUpgraded = newTier.ordinal > oldTier.ordinal
                    tierDowngraded = newTier.ordinal < oldTier.ordinal
                    // Refresh user after tier update
                    user = userRepository.findById(id).orElseThrow()
                } catch (e: Exception) {
                    // Log the error but don't fail the login if tier update fails
                    println("Failed to update loyalty tier for user $id: ${e.message}")
                    e.printStackTrace()
                    // Continue with login using existing tier
                }
            }

            val token = TokenUtil.issueFakeToken(id, user.email)

            user.notificationToken = if(req.notificationToken == "") {null} else {req.notificationToken}
            userRepository.save(user)

            return LoginResponse(
                token = token,
                email = userEmail,
                userId = id,
                isRider = isRider,
                isOperator = isOperator,
                pricingPlan = userPricingPlan,
                flexDollars = userFlexDollars,
                kilometersTravelled = user.kilometersTravelled,
                loyaltyTier = user.loyaltyTier.name,
                loyaltyTierDisplayName = user.loyaltyTier.displayName,
                tierChanged = tierChanged,
                tierUpgraded = tierUpgraded,
                tierDowngraded = tierDowngraded,
                oldTier = if (tierChanged) oldTier.name else null,
                newTier = if (tierChanged) user.loyaltyTier.name else null
            )
        } catch (e: Exception) {
            println("Login failed with exception: ${e.javaClass.name}: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

class EmailAlreadyUsedException : RuntimeException("EMAIL_IN_USE: " +
        "This email is already used. Please use another email or login with this one.")

class UsernameAlreadyUsedException :
    RuntimeException("USERNAME_IN_USE: This username is already taken. Choose another one.")

class InvalidCredentialsException :
    RuntimeException("INVALID_CREDENTIALS: Email or password is incorrect.")
