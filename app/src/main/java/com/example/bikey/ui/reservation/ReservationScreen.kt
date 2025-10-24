package com.example.bikey.ui.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike // Now correctly imported
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bikey.ui.bmscoreandstationcontrol.model.Bicycle
import com.example.bikey.ui.bmscoreandstationcontrol.model.EBike
import com.example.bikey.ui.theme.* // Import your theme colors, including the new ones
import java.util.*

/**
 * Main entry point for the demo. Call this from your MainActivity or a Preview.
 */
@Composable
fun ReservationScreenDemo(viewModel: ReservationViewModel = viewModel()) {
    ReservationScreen(
        viewModel = viewModel,
        onReservationSuccess = { /* Handle navigation or state change */ },
        onGoBack = { /* Handle back navigation */ }
    )
}

@OptIn(ExperimentalMaterial3Api::class) // Opt-in for Scaffold and TopAppBar
@Composable
fun ReservationScreen(
    viewModel: ReservationViewModel,
    onGoBack: () -> Unit,
    onReservationSuccess: () -> Unit
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    // Load the hardcoded station data when the screen first appears
    LaunchedEffect(Unit) {
        viewModel.loadDefaultStationData()
    }

    // Listen for events from the ViewModel to show snackbars
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReservationEvent.Success -> {
                    snackbarHostState.showSnackbar("Bike Reserved Successfully!", duration = SnackbarDuration.Short)
                    kotlinx.coroutines.delay(1500) // Wait a moment before callback
                    onReservationSuccess()
                }
                is ReservationEvent.Failure -> {
                    snackbarHostState.showSnackbar(event.message, withDismissAction = true)
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
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 16.dp)) },
            topBar = {
                TopAppBar(
                    title = { Text("Reserve a Bike") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display station info only if it's loaded
                state.dockingStation?.let { station ->
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = EcoGreen),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${station.address.number} ${station.address.street}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (state.availableBikes.isEmpty()) {
                    Text("No bikes available for reservation.", modifier = Modifier.padding(32.dp))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(state.availableBikes, key = { it.id }) { bike ->
                            BikeCard(
                                bike = bike,
                                isBeingReserved = state.isLoading && state.reservationSuccessBikeId != bike.id,
                                isSuccessfullyReserved = state.reservationSuccessBikeId == bike.id,
                                onReserveClick = {
                                    // Prevent clicking another while one is processing
                                    if (!state.isLoading) {
                                        viewModel.reserveBike(bike.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BikeCard(
    bike: Bicycle,
    isBeingReserved: Boolean,
    isSuccessfullyReserved: Boolean,
    onReserveClick: () -> Unit
) {
    val isEBike = bike is EBike
    val bikeType = if (isEBike) "E-Bike" else "Standard Bike"
    // Use the newly defined color
    val cardColor = if (isSuccessfullyReserved) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = "Bike Icon",
                    // Use the newly defined colors
                    tint = if (isEBike) ElectricBlue else EcoGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = bikeType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "ID: ${bike.id.toString().uppercase().take(8)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Box(modifier = Modifier.size(width = 100.dp, height = 40.dp), contentAlignment = Alignment.Center) {
                when {
                    isSuccessfullyReserved -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Reserved",
                            // Use the newly defined color
                            tint = SuccessGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    isBeingReserved -> {
                        CircularProgressIndicator(strokeWidth = 3.dp, color = EcoGreen)
                    }
                    else -> {
                        Button(
                            onClick = onReserveClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reserve")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReservationScreenPreview() {
    ReservationScreenDemo()
}