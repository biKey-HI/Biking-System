// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
// import org.springframework.stereotype.Service

// @Service
// class UserService(private val userRepository: UserRepository) {
//     private val passwordEncoder = BCryptPasswordEncoder()

//     fun registerUser(userDTO: UserRegistrationDTO): String {
//         // Check if the user already exists
//         if (userRepository.findByEmail(userDTO.email) != null) {
//             return "Email already in use."
//         }

//         // Encrypt the password before saving
//         val encodedPassword = passwordEncoder.encode(userDTO.password)

//         // Save the new user
//         val user = User(email = userDTO.email, password = encodedPassword)
//         userRepository.save(user)

//         return "User registered successfully."
//     }
// }
