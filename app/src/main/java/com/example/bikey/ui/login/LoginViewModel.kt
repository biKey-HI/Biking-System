package com.example.bikey.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikey.ui.User
import com.example.bikey.ui.UserContext
import com.example.bikey.ui.login.model.LoginRequest
import com.example.bikey.ui.login.model.LoginResponse
import com.example.bikey.ui.network.AuthApi
import com.example.bikey.ui.network.authApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val successMsg: String? = null,
    val successEmail: String? = null,
    val userRole: String? = null
)

sealed interface LoginEvent {
    data class Success(val msg: String, val email: String, val role: String) : LoginEvent
    data class ShowMessage(val message: String) : LoginEvent
    data class NavigateHome(val email: String, val role: String) : LoginEvent
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

        // Enhanced validation
        when {
            e.isBlank() -> {
                set { copy(errorMsg = "Email is required.") }
                return
            }
            !isValidEmail(e) -> {
                set { copy(errorMsg = "Please enter a valid email address.") }
                return
            }
            p.isBlank() -> {
                set { copy(errorMsg = "Password is required.") }
                return
            }
            p.length < 8 -> {
                set { copy(errorMsg = "Password must be at least 8 characters long.") }
                return
            }
            p.length > 72 -> {
                set { copy(errorMsg = "Password must be less than 72 characters long.") }
                return
            }
        }

        set { copy(isLoading = true, errorMsg = null, successMsg = null) }

        viewModelScope.launch {
            try {
                var notificationToken = ""
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) return@addOnCompleteListener
                    notificationToken = task.result
                }
                val res = api.login(LoginRequest(e, p, notificationToken))
                if (res.isSuccessful) {
                    val body: LoginResponse? = res.body()
                    if (body != null) {
                        authStore?.saveToken(body.token)
                        UserContext.notificationToken = if(notificationToken == "") {null} else {notificationToken}


                        set { copy(isLoading = false, successMsg = "Successfully logged in!", successEmail = body.email, userRole = body.role) }

                        UserContext.user = User(body.userId, body.email, body.role == "OPERATOR", pricingPlan = body.pricingPlan, flexDollars = body.flexDollars)

                        _events.emit(LoginEvent.Success("Welcome back!", body.email, body.role))
                    } else {
                        val err = "Invalid server response. Please try again."
                        set { copy(isLoading = false, errorMsg = err) }
                        _events.emit(LoginEvent.ShowMessage(err))
                    }
                } else if (res.code() == 401) {
                    val err = "Invalid email or password. Please check your credentials and try again."
                    set { copy(isLoading = false, errorMsg = err) }
                    _events.emit(LoginEvent.ShowMessage(err))
                } else if (res.code() == 404) {
                    val err = "No account found with this email address. Please register first."
                    set { copy(isLoading = false, errorMsg = err) }
                    _events.emit(LoginEvent.ShowMessage(err))
                } else {
                    val err = "Login failed. Please try again later. (Error ${res.code()})"
                    set { copy(isLoading = false, errorMsg = err) }
                    _events.emit(LoginEvent.ShowMessage(err))
                }
            } catch (ex: Exception) {
                val err = "Network error. Please check your connection and try again."
                set { copy(isLoading = false, errorMsg = err) }
                _events.emit(LoginEvent.ShowMessage(err))
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return email.matches(emailPattern.toRegex()) && email.length <= 254
    }
}
