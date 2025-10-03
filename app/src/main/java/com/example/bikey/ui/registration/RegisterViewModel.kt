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
    fun onAddrPostalChange(v: String) = set { copy(addrPostal = v, error = null) }
    fun onAddrCountryChange(v: String) = set { copy(addrCountry = v, error = null) }

    fun onPayHolderChange(v: String) = set { copy(payHolder = v, error = null) }
    fun onPayCardNumberChange(v: String) = set { copy(payCardNumber = v.filter { it.isDigit() }, error = null) }
    fun onPayCvv3Change(v: String) = set { copy(payCvv3 = v.filter { it.isDigit() }.take(3), error = null) }

    // nav helpers
    fun next() = set { copy(step = (step + 1).coerceAtMost(2)) }
    fun back() = set { copy(step = (step - 1).coerceAtLeast(0)) }

    fun submit() {

        Log.d("RegisterVM", "submit() tapped. email='${state.email}', firstname='${state.firstName}'," +
                "lastname='${state.lastName}', usernamename='${state.username}', role='${state.role}'")

        if (state.step < 2) {
            next()
            return
        }

        val email = state.email.trim()
        val pwd = state.password
        val firstname = state.firstName.trim()
        val lastname = state.lastName.trim()
        val usernamename = state.username.trim()
        val role = state.role

        if (!email.contains("@") || pwd.length < 8) {
            set { copy(error = "Invalid email or password < 8 characters") }
            Log.w("RegisterVM", "Client-side validation failed")

            return
        }

        if (!email.contains("@") || pwd.length < 8) {
            set { copy(error = "Invalid email or password < 8 characters") }
            return
        }

        // Build new payloads from state
        val address = AddressPayload(
            line1 = state.addrLine1.trim(),
            line2 = state.addrLine2.trim().ifEmpty { null },
            city = state.addrCity.trim(),
            province = state.addrProvince,
            postalCode = state.addrPostal.trim(),
            country = state.addrCountry.trim()
        )

        // If payment fields are blank, send null (skip)
        val payment = if (state.payHolder.isBlank() && state.payCardNumber.isBlank() && state.payCvv3.isBlank()) {
            null
        } else {
            PaymentPayload(
                cardHolderName = state.payHolder.trim(),
                cardNumber = state.payCardNumber.trim(),
                cvv3 = state.payCvv3.trim()
            )
        }

        viewModelScope.launch {
            set { copy(isLoading = true, error = null, successEmail = null) }
            try {
                // Use named params so there’s no ambiguity
                Log.d("RegisterVM", "Calling API…")
                val res = api.register(RegisterRequest(email = email, password = pwd, firstName = firstname,
                    lastName = lastname, username = usernamename, role = role, address = address,
                    payment = payment))
                if (res.isSuccessful) {
                    Log.i("RegisterVM", "SUCCESS ${res.code()} – will clear fields and set successMessage")
                    set {
                        copy(
                            isLoading = false,
                            successEmail = email,
                            successMessage = "Successful Registration, $usernamename ! Your account is ready.",
                            email = "",
                            password = "",
                            firstName = "",
                            lastName = "",
                            username = "",
                            role = UserRole.RIDER,
                            step = 0,
                            addrLine1 = "", addrLine2 = "", addrCity = "",
                            addrProvince = Province.QC, addrPostal = "", addrCountry = "CA",
                            payHolder = "", payCardNumber = "", payCvv3 = ""
                        )
                    }
                    _events.emit(RegisterEvent.Success(email))
                } else if (res.code() == 409) {
                    set { copy(isLoading = false, error = "Account exists. Please log in.") }
                    _events.emit(RegisterEvent.EmailInUse)
                } else {
                    val errorBody = res.errorBody()?.string().orEmpty()
                    val msg = buildString {
                        append("HTTP ${res.code()} ${res.message()}")
                        if (errorBody.isNotBlank()) append(": $errorBody")
                    }
                    // debug log to Logcat
                    Log.e("RegisterViewModel", "Registration failed: $msg")
                    set { copy(isLoading = false, error = msg) }
                    _events.emit(RegisterEvent.Failure(msg))
                }
            } catch (e: Exception) {
                Log.e("RegisterVM", "EXCEPTION during register", e)
                val msg = if (e.message?.contains("409") == true) "Email already used" else "Error: ${e.message}"
                set { copy(isLoading = false, error = msg) }
                _events.emit(RegisterEvent.Failure(msg))
            }
        }
    }
    fun consumeSuccess() = set { copy(successEmail = null, successMessage = null) }
}
