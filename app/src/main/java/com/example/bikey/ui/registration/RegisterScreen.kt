// This is the registration form's UI. It creates/gets a RegisterViewModel, then displays text
// fields for email and password, a submit button, and reacts to state (loading, error, success).
// Then, on success, it calls the onRegistered(email) callback
// (so MainActivity can navigate, e.g., to "home" later).
package com.example.bikey.ui.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bikey.ui.theme.*
import com.example.bikey.ui.registration.model.Province

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onGoToLogin: () -> Unit = {},
    onGoBack: () -> Unit = {},
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LightGreen.copy(alpha = 0.1f),
                        PureWhite,
                        MintLight.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        // Back button
        IconButton(
            onClick = onGoBack,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Welcome",
                tint = EcoGreen
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState()), // Make column scrollable
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = PureWhite
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title section
                    Text(
                        text = when (state.step) {
                            0 -> "Create Account"
                            1 -> "Your Address"
                            else -> "Payment"
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = EcoGreen
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (state.step) {
                            0 -> "Join BiKey for eco-friendly biking"
                            1 -> "Where do we send billing and correspondence?"
                            else -> "Optional — add payment now or skip for later"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))


                    Text(
                        text = "Step ${state.step + 1} of 3",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Step content
                    when (state.step) {
                        0 -> {
                            // Personal info (same fields as before)
                            OutlinedTextField(
                                value = state.firstName,
                                onValueChange = viewModel::onfirstNameChange,
                                label = { Text("First Name") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = "First Name", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.lastName,
                                onValueChange = viewModel::onlastNameChange,
                                label = { Text("Last Name") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Last Name", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.username,
                                onValueChange = viewModel::onUsernameChange,
                                label = { Text("Username") },
                                leadingIcon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Username", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.email,
                                onValueChange = viewModel::onEmailChange,
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.password,
                                onValueChange = viewModel::onPasswordChange,
                                label = { Text("Password (min 8 characters)") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = EcoGreen) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Next button
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(onClick = viewModel::next, enabled = !state.isLoading, colors = ButtonDefaults.buttonColors(containerColor = EcoGreen), shape = RoundedCornerShape(16.dp)) {
                                    Text("Next", color = PureWhite)
                                }
                            }
                        }

                        1 -> {
                            // Address step
                            OutlinedTextField(
                                value = state.addrLine1,
                                onValueChange = viewModel::onAddrLine1Change,
                                label = { Text("Address Line 1") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Address", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.addrLine2,
                                onValueChange = viewModel::onAddrLine2Change,
                                label = { Text("Address Line 2 (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.addrCity,
                                onValueChange = viewModel::onAddrCityChange,
                                label = { Text("City") },
                                leadingIcon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = "City", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Province dropdown
                            var expanded by remember { mutableStateOf(false) }
                            val provinces = Province.values()
                            OutlinedTextField(
                                value = state.addrProvince.name,
                                onValueChange = {},
                                label = { Text("Province") },
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Choose province")
                                    }
                                },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                provinces.forEach { p ->
                                    DropdownMenuItem(text = { Text(p.name) }, onClick = {
                                        viewModel.onAddrProvinceChange(p)
                                        expanded = false
                                    })
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.addrPostal,
                                onValueChange = viewModel::onAddrPostalChange,
                                label = { Text("Postal Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Button(onClick = viewModel::previousStep, enabled = !state.isLoading, colors = ButtonDefaults.buttonColors(containerColor = DarkGreen), shape = RoundedCornerShape(16.dp)) {
                                    Text("Back", color = PureWhite)
                                }
                                Button(onClick = viewModel::next, enabled = !state.isLoading, colors = ButtonDefaults.buttonColors(containerColor = EcoGreen), shape = RoundedCornerShape(16.dp)) {
                                    Text("Next", color = PureWhite)
                                }
                            }
                        }

                        2 -> {
                            // Payment step (optional)
                            OutlinedTextField(
                                value = state.payHolder,
                                onValueChange = viewModel::onPayHolderChange,
                                label = { Text("Cardholder Name") },
                                leadingIcon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Cardholder", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.payCardNumber,
                                onValueChange = viewModel::onPayCardNumberChange,
                                label = { Text("Card Number") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Info, contentDescription = "Card", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.payCvv3,
                                onValueChange = viewModel::onPayCvv3Change,
                                label = { Text("CVV") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "CVV", tint = EcoGreen) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcoGreen,
                                    focusedLabelColor = EcoGreen
                                )
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Button(onClick = viewModel::previousStep, enabled = !state.isLoading, colors = ButtonDefaults.buttonColors(containerColor = DarkGreen), shape = RoundedCornerShape(16.dp)) {
                                    Text("Back", color = PureWhite)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    // Skip for now
                                    TextButton(onClick = { viewModel.skipPayment() }) {
                                        Text("Skip for now", color = EcoGreen)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Create account
                                    Button(
                                        onClick = { viewModel.submit(skipPayment = false) },
                                        enabled = !state.isLoading,
                                        modifier = Modifier.height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EcoGreen)
                                    ) {
                                        if (state.isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PureWhite)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Creating account...", color = PureWhite)
                                        } else {
                                            Text("Create Account", color = PureWhite)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Error message (kept at bottom of card)
                    if (!state.error.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = state.error ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Small footer link to login
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = EcoGreen
                    ),
                    modifier = Modifier.clickable { onGoToLogin() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
