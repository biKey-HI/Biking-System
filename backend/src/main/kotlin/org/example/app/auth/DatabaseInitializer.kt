package org.example.app.auth

import org.example.app.user.*
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class DatabaseInitializer(
    private val userRepository: UserRepository,
    private val addressRepository: AddressRepository
) : CommandLineRunner {

    private val encoder = BCryptPasswordEncoder()

    override fun run(vararg args: String?) {
        // Create predefined operator accounts
        createOperatorIfNotExists(
            email = "operator@bikingsystem.com",
            password = "operator123",
            firstName = "System",
            lastName = "Operator",
            username = "operator1"
        )

        createOperatorIfNotExists(
            email = "admin@bikingsystem.com",
            password = "admin123",
            firstName = "Admin",
            lastName = "User",
            username = "admin1"
        )
        createDualOperatorIfNotExists(
            email = "dual@bikey.com",
            password = "dual123",
            firstName = "Dual",
            lastName = "User",
            username = "dual1"
        )

    }

    private fun createOperatorIfNotExists(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        username: String
    ) {
        if (!userRepository.existsByEmail(email)) {
            val address = addressRepository.findByLine1AndLine2AndCityAndProvinceAndPostalCodeAndCountry(
                "123 System St",
                null,
                "Toronto",
                Province.ON,
                "M5V 3A8",
                "CA"
            ) ?: addressRepository.save(
                Address(
                    line1 = "123 System St",
                    line2 = null,
                    city = "Toronto",
                    province = Province.ON,
                    postalCode = "M5V 3A8",
                    country = "CA"
                )
            )

            userRepository.save(
                User(
                    id = null,
                    address = address,
                    payment = null,
                    email = email,
                    passwordHash = encoder.encode(password),
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    createdAt = java.time.Instant.now(),
                    isRider = false,
                    isOperator = true
                )
            )
            println("Created operator account: $email")
        }
    }
    private fun createDualOperatorIfNotExists(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        username: String
    ) {
        if (!userRepository.existsByEmail(email)) {
            val address = addressRepository.findByLine1AndLine2AndCityAndProvinceAndPostalCodeAndCountry(
                "123 System St",
                null,
                "Toronto",
                Province.ON,
                "M5V 3A8",
                "CA"
            ) ?: addressRepository.save(
                Address(
                    line1 = "123 System St",
                    line2 = null,
                    city = "Toronto",
                    province = Province.ON,
                    postalCode = "M5V 3A8",
                    country = "CA"
                )
            )

            userRepository.save(
                User(
                    id = null,
                    address = address,
                    payment = null,
                    email = email,
                    passwordHash = encoder.encode(password),
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    createdAt = java.time.Instant.now(),
                    isRider = true,
                    isOperator = true
                )
            )
            println("Created dual-operator account: $email")
        }
    }
}


