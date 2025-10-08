package com.example.bikey.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onGoToRegister: () -> Unit = {},
    onLoggedIn: (String) -> Unit = {},
    onForgotPassword: () -> Unit = {},
    vm: LoginViewModel = viewModel()
) {
    val state = vm.state
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMsg) {
        state.errorMsg?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is LoginEvent.Success -> {
                    snackbarHostState.showSnackbar(message = event.msg, withDismissAction = true)
                    onLoggedIn(event.email)
                    vm.consumeSuccess()
                }
                is LoginEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is LoginEvent.NavigateHome -> {
                    onLoggedIn(event.email)
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Log in to your account", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.email,
                    onValueChange = vm::onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = vm::onPasswordChange,
                    label = { Text("Password (min 8)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onForgotPassword() }
                )


                Text(
                    text = "Don't have an account yet? Register",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onGoToRegister() }
                )
                Spacer(Modifier.height(8.dp))

                if (!state.errorMsg.isNullOrBlank()) {
                    Text(
                        text = state.errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = vm::submit,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Logging in…")
                    } else {
                        Text("Log in")
                    }
                }
            }
        }
    }
}
