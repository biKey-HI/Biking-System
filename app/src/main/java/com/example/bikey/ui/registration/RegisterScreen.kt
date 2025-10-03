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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.bikey.ui.registration.model.Province
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.SnackbarDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onGoToLogin: () -> Unit = {},
    onRegistered: (String) -> Unit = {},
    viewModel: RegisterViewModel = viewModel()
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }
    var provinceMenuExpanded by remember { mutableStateOf(false) }
    val provinces = remember { Province.values().toList() }

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

                Text(
                    text = "Step ${state.step + 1} of 3",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (state.step == 0) {
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

//                Text("Choose role", style = MaterialTheme.typography.bodyLarge)
//                Row {
//                    RadioButton(
//                        selected = state.role == UserRole.RIDER,
//                        onClick = { viewModel.onRoleChange(UserRole.RIDER) }
//                    )
//                    Text(
//                        "Rider", modifier = Modifier
//                            .padding(end = 16.dp)
//                            .clickable { viewModel.onRoleChange(UserRole.RIDER) })
//
//                    RadioButton(
//                        selected = state.role == UserRole.OPERATOR,
//                        onClick = { viewModel.onRoleChange(UserRole.OPERATOR) }
//                    )
//                    Text(
//                        "Operator", modifier = Modifier
//                            .clickable { viewModel.onRoleChange(UserRole.OPERATOR) })
//                }

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
            }

                if (state.step == 1) {
                    OutlinedTextField(
                        value = state.addrLine1,
                        onValueChange = viewModel::onAddrLine1Change,
                        label = { Text("Address line 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.addrLine2,
                        onValueChange = viewModel::onAddrLine2Change,
                        label = { Text("Address line 2 (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.addrCity,
                        onValueChange = viewModel::onAddrCityChange,
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Province dropdown
                    ExposedDropdownMenuBox(
                        expanded = provinceMenuExpanded,
                        onExpandedChange = { provinceMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.addrProvince.name,
                            onValueChange = {}, // read-only
                            readOnly = true,
                            label = { Text("Province") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = provinceMenuExpanded,
                            onDismissRequest = { provinceMenuExpanded = false }
                        ) {
                            provinces.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        viewModel.onAddrProvinceChange(p)
                                        provinceMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.addrPostal,
                        onValueChange = viewModel::onAddrPostalChange,
                        label = { Text("Postal code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.addrCountry,
                        onValueChange = viewModel::onAddrCountryChange,
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.step == 2) {
                    OutlinedTextField(
                        value = state.payHolder,
                        onValueChange = viewModel::onPayHolderChange,
                        label = { Text("Card holder name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.payCardNumber,
                        onValueChange = viewModel::onPayCardNumberChange,
                        label = { Text("Card number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.payCvv3,
                        onValueChange = viewModel::onPayCvv3Change,
                        label = { Text("CVV") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Tip: Leave all payment fields blank to skip.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

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
                        Text(if (state.step < 2) "Next" else "Register")
                    }
                }
            }
        }
    }
}

