package com.example.bikey.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikey.ui.login.model.LoginRequest
import com.example.bikey.ui.login.model.LoginResponse
import com.example.bikey.ui.network.AuthApi
import com.example.bikey.ui.network.authApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val successMsg: String? = null,
    val successEmail: String? = null
)

sealed interface LoginEvent {
    data class Success(val msg: String, val email: String) : LoginEvent
    data class ShowMessage(val message: String) : LoginEvent
    data class NavigateHome(val email: String) : LoginEvent
}


class LoginViewModel(
    private val authStore: AuthStore? = null,
    private val api: AuthApi = authApi
) : ViewModel() {

    private val _events = MutableSharedFlow<LoginEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    var state by mutableStateOf(LoginState())
        private set

    private fun set(upd: LoginState.() -> LoginState) { state = state.upd() }

    fun onEmailChange(s: String) = set { copy(email = s, errorMsg = null) }
    fun onPasswordChange(s: String) = set { copy(password = s, errorMsg = null) }
    fun consumeSuccess() = set { copy(successMsg = null, successEmail = null) }

    fun submit() {
        val e = state.email.trim()
        val p = state.password

        if (e.isBlank() || !e.contains("@") || p.length !in 8..72) {
            set { copy(errorMsg = "Enter a valid email and 8–72 char password.") }
            return
        }

        set { copy(isLoading = true, errorMsg = null, successMsg = null) }

        viewModelScope.launch {
            try {
                val res = api.login(LoginRequest(e, p))
                if (res.isSuccessful) {
                    val body: LoginResponse? = res.body()
                    if (body != null) {
                        authStore?.saveToken(body.token)  // this is safe when authStore is null

                        set { copy(isLoading = false, successMsg = "Logged in!", successEmail = body.email) }

                        _events.emit(LoginEvent.Success("Logged in!", body.email))
                    } else {
                        val err = "Empty response from server."
                        set { copy(isLoading = false, errorMsg = err) }
                        _events.emit(LoginEvent.ShowMessage(err))
                    }
                } else if (res.code() == 401) {
                    val err = "Wrong email or password."
                    set { copy(isLoading = false, errorMsg = err) }
                    _events.emit(LoginEvent.ShowMessage(err))
                } else {
                    val err = "Login failed: ${res.code()} ${res.message()}"
                    set { copy(isLoading = false, errorMsg = err) }
                    _events.emit(LoginEvent.ShowMessage(err))
                }
            } catch (ex: Exception) {
                val err = "Network error: ${ex.message ?: "unknown"}"
                set { copy(isLoading = false, errorMsg = err) }
                _events.emit(LoginEvent.ShowMessage(err))
            }
        }
    }
}

