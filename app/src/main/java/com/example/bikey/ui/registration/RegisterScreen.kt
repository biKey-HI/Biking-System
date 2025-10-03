// This is the registration form's UI. It creates/gets a RegisterViewModel, then displays text
// fields for email and password, a submit button, and reacts to state (loading, error, success).
// Then, on success, it calls the onRegistered(email) callback
// (so MainActivity can navigate, e.g., to “home” later).
package com.example.bikey.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.bikey.ui.registration.model.UserRole
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.SnackbarDuration

@Composable
fun RegisterScreen(
    onGoToLogin: () -> Unit = {},
    onRegistered: (String) -> Unit = {},
    viewModel: RegisterViewModel = viewModel()
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    // Collect events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RegisterEvent.EmailInUse -> {
                    snackbarHostState.showSnackbar(
                        "Account exists. Redirecting to login…",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    onGoToLogin()
                }
                is RegisterEvent.Success -> {
                    snackbarHostState.showSnackbar(
                        "Registered as ${event.email}",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    onRegistered(event.email)
                }
                is RegisterEvent.Failure -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
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
                Text("Create an account", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))

                // First Name
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::onfirstNameChange,
                    label = { Text("First name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Last Name
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::onlastNameChange,
                    label = { Text("Last name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Username
                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Choose role", style = MaterialTheme.typography.bodyLarge)
                Row {
                    RadioButton(
                        selected = state.role == UserRole.RIDER,
                        onClick = { viewModel.onRoleChange(UserRole.RIDER) }
                    )
                    Text("Rider", modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable { viewModel.onRoleChange(UserRole.RIDER) })

                    RadioButton(
                        selected = state.role == UserRole.OPERATOR,
                        onClick = { viewModel.onRoleChange(UserRole.OPERATOR) }
                    )
                    Text("Operator", modifier = Modifier
                        .clickable { viewModel.onRoleChange(UserRole.OPERATOR) })
                }

                // Email
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Password
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password (min 8)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Already have an account? Log in",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onGoToLogin() }
                )
                Spacer(Modifier.height(8.dp))

                if (!state.error.isNullOrBlank()) {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = viewModel::submit,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Registering…")
                    } else {
                        Text("Register")
                    }
                }
            }
        }
    }
}

