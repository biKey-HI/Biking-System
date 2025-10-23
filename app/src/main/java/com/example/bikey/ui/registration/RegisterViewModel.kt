// This is like the brain of the registration page. It holds a RegisterUiState
// (email, password, isLoading, error, successEmail). It validates inputs
// (basic checks: email contains @, password length), and on submit(), it launches a coroutine
// (viewModelScope.launch) and calls the network (authApi.register(RegisterRequest(email, password)))
// It's in charge of interpreting the HTTP result.
package com.example.bikey.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikey.ui.registration.model.RegisterRequest
import com.example.bikey.ui.registration.model.UserRole
import com.example.bikey.ui.network.AuthApi
import com.example.bikey.ui.network.authApi
import com.example.bikey.ui.registration.model.Province
import com.example.bikey.ui.registration.model.AddressPayload
import com.example.bikey.ui.registration.model.PaymentPayload
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successEmail: String? = null,
    val successMessage: String? = null,
    val role: UserRole = UserRole.RIDER,

    // (0=user, 1=address, 2=payment)
    val step: Int = 0,


    val addrLine1: String = "",
    val addrLine2: String = "",
    val addrCity: String = "",
    val addrProvince: Province = Province.QC,
    val addrPostal: String = "",
    val addrCountry: String = "CA",


    val payHolder: String = "",
    val payCardNumber: String = "",
    val payCvv3: String = ""
)

sealed class RegisterEvent {
    data object EmailInUse : RegisterEvent()
    data class Success(val email: String) : RegisterEvent()
    data class Failure(val message: String) : RegisterEvent()
}
class RegisterViewModel(
    private val api: AuthApi = authApi
) : ViewModel() {
//        var state: LoginState = LoginState()
//        private set
private val _events = MutableSharedFlow<RegisterEvent>()
    val events = _events.asSharedFlow()


    var state by mutableStateOf(RegisterUiState())
        private set

    private fun set(block: RegisterUiState.() -> RegisterUiState) {
        state = state.block()
    }

    fun onEmailChange(v: String) = set { copy(email = v, error = null) }
    fun onPasswordChange(v: String) = set { copy(password = v, error = null) }
    fun onfirstNameChange(v: String) = set { copy(firstName = v, error = null) }
    fun onlastNameChange(v: String) = set { copy(lastName = v, error = null) }
    fun onUsernameChange(v: String) = set { copy(username = v, error = null) }
    fun onRoleChange(r: UserRole) = set { copy(role = r, error = null) }

    fun onAddrLine1Change(v: String) = set { copy(addrLine1 = v, error = null) }
    fun onAddrLine2Change(v: String) = set { copy(addrLine2 = v, error = null) }
    fun onAddrCityChange(v: String)  = set { copy(addrCity = v, error = null) }
    fun onAddrProvinceChange(p: Province) = set { copy(addrProvince = p, error = null) }
    fun onAddrPostalChange(v: String) = set {
        // Only allow alphanumeric for postal code, max 7 chars (6 + optional space)
        val filtered = v.filter { it.isLetterOrDigit() || it == ' ' }.take(7).uppercase()
        copy(addrPostal = filtered, error = null)
    }
    fun onAddrCountryChange(v: String) = set { copy(addrCountry = v, error = null) }

    fun onPayHolderChange(v: String) = set { copy(payHolder = v, error = null) }
    fun onPayCardNumberChange(v: String) = set { copy(payCardNumber = v.filter { it.isDigit() }, error = null) }
    fun onPayCvv3Change(v: String) = set { copy(payCvv3 = v.filter { it.isDigit() }.take(3), error = null) }

    // nav helpers with validation
    fun next() {
        // Validate current step before moving to next
        when (state.step) {
            0 -> {
                // Validate personal info
                val email = state.email.trim()
                val pwd = state.password
                val firstname = state.firstName.trim()
                val lastname = state.lastName.trim()
                val username = state.username.trim()

                when {
                    firstname.isBlank() -> {
                        set { copy(error = "Please enter your first name") }
                        return
                    }
                    firstname.length < 2 -> {
                        set { copy(error = "First name must be at least 2 characters") }
                        return
                    }
                    lastname.isBlank() -> {
                        set { copy(error = "Please enter your last name") }
                        return
                    }
                    lastname.length < 2 -> {
                        set { copy(error = "Last name must be at least 2 characters") }
                        return
                    }
                    username.isBlank() -> {
                        set { copy(error = "Please enter a username") }
                        return
                    }
                    username.length < 3 -> {
                        set { copy(error = "Username must be at least 3 characters") }
                        return
                    }
                    email.isBlank() || !email.contains("@") || !email.contains(".") -> {
                        set { copy(error = "Please enter a valid email address") }
                        return
                    }
                    pwd.length < 8 -> {
                        set { copy(error = "Password must be at least 8 characters") }
                        return
                    }
                }
            }
            1 -> {
                // Validate address
                val line1 = state.addrLine1.trim()
                val city = state.addrCity.trim()
                val postal = state.addrPostal.trim().replace(" ", "")

                when {
                    line1.isBlank() -> {
                        set { copy(error = "Please enter your street address") }
                        return
                    }
                    line1.length < 5 -> {
                        set { copy(error = "Please enter a complete street address") }
                        return
                    }
                    city.isBlank() -> {
                        set { copy(error = "Please enter your city") }
                        return
                    }
                    city.length < 2 -> {
                        set { copy(error = "Please enter a valid city name") }
                        return
                    }
                    postal.isBlank() -> {
                        set { copy(error = "Please enter your postal code") }
                        return
                    }
                    postal.length != 6 -> {
                        set { copy(error = "Postal code must be 6 characters (e.g., H3A2B4)") }
                        return
                    }
                }
            }
        }
        // If validation passes, move to next step
        set { copy(step = (step + 1).coerceAtMost(2), error = null) }
    }

    fun previousStep() = set { copy(step = (step - 1).coerceAtLeast(0), error = null) }

    // Allow skipping payment step: submit(skipPayment = true) will send payment = null
    fun skipPayment() {
        submit(skipPayment = true)
    }

    fun submit(skipPayment: Boolean = false) {
        Log.d("RegisterVM", "submit() tapped. email='${state.email}', firstname='${state.firstName}'," +
                "lastname='${state.lastName}', username='${state.username}', role='${state.role}'")

        val email = state.email.trim()
        val pwd = state.password
        val firstname = state.firstName.trim()
        val lastname = state.lastName.trim()
        val username = state.username.trim()
        val role = state.role

        // Basic validation
        if (email.isBlank() || !email.contains("@")) {
            set { copy(error = "Please enter a valid email address") }
            return
        }

        if (pwd.length < 8) {
            set { copy(error = "Password must be at least 8 characters") }
            return
        }

        if (firstname.isBlank()) {
            set { copy(error = "Please enter your first name") }
            return
        }

        if (lastname.isBlank()) {
            set { copy(error = "Please enter your last name") }
            return
        }

        if (username.isBlank()) {
            set { copy(error = "Please enter a username") }
            return
        }

        // Build address payload from state (user must have filled address page before submitting)
        val address = AddressPayload(
            line1 = state.addrLine1.trim(),
            line2 = state.addrLine2.trim().ifEmpty { null },
            city = state.addrCity.trim(),
            province = state.addrProvince,
            postalCode = state.addrPostal.trim(),
            country = state.addrCountry.trim().ifEmpty { "CA" }
        )

        // Build payment payload if not skipped and a holder name is present
        val payment = if (skipPayment) {
            null
        } else {
            if (state.payHolder.isBlank()) null else PaymentPayload(
                cardHolderName = state.payHolder.trim(),
                provider = null,
                token = null,
                cardBrand = null,
                cardLast4 = state.payCardNumber.takeLast(4).ifEmpty { null },
                cardNumber = state.payCardNumber
            )
        }

        viewModelScope.launch {
            set { copy(isLoading = true, error = null, successEmail = null) }
            try {
                Log.d("RegisterVM", "Calling API…")
                val res = api.register(RegisterRequest(
                    email = email,
                    password = pwd,
                    firstName = firstname,
                    lastName = lastname,
                    username = username,
                    role = role,
                    address = address,
                    payment = payment
                ))

                if (res.isSuccessful) {
                    Log.i("RegisterVM", "SUCCESS ${res.code()} – registration complete")
                    set {
                        copy(
                            isLoading = false,
                            successEmail = email,
                            successMessage = "Welcome to BiKey, $username! Your account is ready.",
                            email = "",
                            password = "",
                            firstName = "",
                            lastName = "",
                            username = "",
                            role = UserRole.RIDER,
                            // reset steps + address/payment
                            step = 0,
                            addrLine1 = "",
                            addrLine2 = "",
                            addrCity = "",
                            addrProvince = Province.QC,
                            addrPostal = "",
                            addrCountry = "CA",
                            payHolder = "",
                            payCardNumber = "",
                            payCvv3 = ""
                        )
                    }
                    _events.emit(RegisterEvent.Success(email))
                } else if (res.code() == 409) {
                    set { copy(isLoading = false, error = "Account already exists. Please log in instead.") }
                    _events.emit(RegisterEvent.EmailInUse)
                } else {
                    val errorBody = res.errorBody()?.string().orEmpty()
                    val msg = buildString {
                        append("Registration failed: HTTP ${res.code()}")
                        if (errorBody.isNotBlank()) append(" - $errorBody")
                    }
                    Log.e("RegisterViewModel", "Registration failed: $msg")
                    set { copy(isLoading = false, error = msg) }
                    _events.emit(RegisterEvent.Failure(msg))
                }
            } catch (e: Exception) {
                Log.e("RegisterVM", "EXCEPTION during register", e)
                val msg = if (e.message?.contains("409") == true) {
                    "Email already registered. Please log in instead."
                } else {
                    "Registration failed: ${e.message ?: "Unknown error"}"
                }
                set { copy(isLoading = false, error = msg) }
                _events.emit(RegisterEvent.Failure(msg))
            }
        }
    }
    fun consumeSuccess() = set { copy(successEmail = null, successMessage = null) }
}
