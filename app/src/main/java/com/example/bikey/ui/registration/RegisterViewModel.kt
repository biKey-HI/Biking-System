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

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successEmail: String? = null
)

class RegisterViewModel(
    private val api: AuthApi = authApi // <-- use the provided singleton
) : ViewModel() {

    // make it observable by Compose
    var state by mutableStateOf(RegisterUiState())
        private set

    private fun set(block: RegisterUiState.() -> RegisterUiState) {
        state = state.block()
    }

    fun onEmailChange(v: String) = set { copy(email = v, error = null) }
    fun onPasswordChange(v: String) = set { copy(password = v, error = null) }

    fun submit() {
        val email = state.email.trim()
        val pwd = state.password
        if (!email.contains("@") || pwd.length < 8) {
            set { copy(error = "Invalid email or password < 8 characters") }
            return
        }
        viewModelScope.launch {
            set { copy(isLoading = true, error = null, successEmail = null) }
            try {
                // Use named params so there’s no ambiguity
                val res = api.register(RegisterRequest(email = email, password = pwd))
                if (res.isSuccessful) {
                    set { copy(isLoading = false, successEmail = email) }
                } else {
                    val msg = if (res.code() == 409) "Email already used" else "Error: ${res.code()}"
                    set { copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                val msg = if (e.message?.contains("409") == true) "Email already used" else "Error: ${e.message}"
                set { copy(isLoading = false, error = msg) }
            }
        }
    }
}
