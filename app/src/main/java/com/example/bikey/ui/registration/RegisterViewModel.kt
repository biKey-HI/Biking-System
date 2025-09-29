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

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successEmail: String? = null,
    val successMessage: String? = null,
)

class RegisterViewModel(
    private val api: AuthApi = authApi
) : ViewModel() {

    // make it observable by Compose
    var state by mutableStateOf(RegisterUiState())
        private set

    private fun set(block: RegisterUiState.() -> RegisterUiState) {
        state = state.block()
    }

    fun onEmailChange(v: String) = set { copy(email = v, error = null) }
    fun onPasswordChange(v: String) = set { copy(password = v, error = null) }
    fun onDisplayNameChange(v: String) = set { copy(displayName = v, error = null) }

    fun submit() {
        Log.d("RegisterVM", "submit() tapped. email='${state.email}', name='${state.displayName}'")

        val email = state.email.trim()
        val pwd = state.password
        val name = state.displayName.trim()

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
                val res = api.register(RegisterRequest(email = email, password = pwd, displayName = name))
                if (res.isSuccessful) {
                    Log.i("RegisterVM", "SUCCESS ${res.code()} – will clear fields and set successMessage")
                    set {
                        copy(
                            isLoading = false,
                            successEmail = email,
                            successMessage = "Successful Registration, $name! Your account is ready.",
                            email = "",
                            password = "",
                            displayName = ""
                        )
                    }
                } else {
                    val errorBody = res.errorBody()?.string().orEmpty()
                    val msg = buildString {
                        append("HTTP ${res.code()} ${res.message()}")
                        if (errorBody.isNotBlank()) append(": $errorBody")
                    }
                    // debug log to Logcat
                    Log.e("RegisterViewModel", "Registration failed: $msg")
                    set { copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                Log.e("RegisterVM", "EXCEPTION during register", e)
                val msg = if (e.message?.contains("409") == true) "Email already used" else "Error: ${e.message}"
                set { copy(isLoading = false, error = msg) }
            }
        }
    }
    fun consumeSuccess() = set { copy(successEmail = null, successMessage = null) }
}
