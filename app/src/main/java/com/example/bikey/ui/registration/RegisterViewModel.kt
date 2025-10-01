// This is like the brain of the registration page. It holds a RegisterUiState
// (email, password, isLoading, error, successEmail). It validates inputs
// (basic checks: email contains @, password length), and on submit(), it launches a coroutine
// (viewModelScope.launch) and calls the network (authApi.register(RegisterRequest(email, password)))
// It's in charge of interpreting the HTTP result.
package com.example.bikey.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikey.ui.registration.model.RegisterRequest
import com.example.bikey.ui.network.AuthApi
import com.example.bikey.ui.network.authApi
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

    fun submit() {
        Log.d("RegisterVM", "submit() tapped. email='${state.email}', firstname='${state.firstName}'," +
                "lastname='${state.lastName}', usernamename='${state.username}'")

        val email = state.email.trim()
        val pwd = state.password
        val firstname = state.firstName.trim()
        val lastname = state.lastName.trim()
        val usernamename = state.username.trim()

        if (!email.contains("@") || pwd.length < 8) {
            set { copy(error = "Invalid email or password < 8 characters") }
            Log.w("RegisterVM", "Client-side validation failed")

            return
        }
        viewModelScope.launch {
            set { copy(isLoading = true, error = null, successEmail = null) }
            try {
                // Use named params so there’s no ambiguity
                Log.d("RegisterVM", "Calling API…")
                val res = api.register(RegisterRequest(email = email, password = pwd, firstName = firstname,
                    lastName = lastname, username = usernamename))
                if (res.isSuccessful) {
                    Log.i("RegisterVM", "SUCCESS ${res.code()} – will clear fields and set successMessage")
                    set {
                        copy(
                            isLoading = false,
                            successEmail = email,
                            successMessage = "Successful Registration, $username ! Your account is ready.",
                            email = "",
                            password = "",
                            firstName = "",
                            lastName = "",
                            username = ""
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
