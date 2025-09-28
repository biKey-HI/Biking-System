package com.example.bikey.ui.registration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikey.ui.registration.model.RegisterRequest
import com.example.bikey.ui.network.authApi
import kotlinx.coroutines.launch


data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successEmail: String? = null
)

class RegisterViewModel(
    private val api: AuthApi = AuthApi.create()
) : ViewModel() {

    var state: RegisterUiState = RegisterUiState()
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
            set { copy(error = "Email invalide ou mot de passe < 8 caractères") }
            return
        }
        viewModelScope.launch {
            set { copy(isLoading = true, error = null, successEmail = null) }
            try {
                val res = api.register(RegisterRequest(email, pwd))
                set { copy(isLoading = false, successEmail = res.email) }
            } catch (e: Exception) {
                val msg = if (e.message?.contains("409") == true) "Email déjà utilisé" else "Erreur: ${e.message}"
                set { copy(isLoading = false, error = msg) }
            }
        }
    }
}
