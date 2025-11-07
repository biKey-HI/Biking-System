package com.example.bikey.ui.rider


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bikey.ui.operator.model.DockingStationResponse
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.bikey.ui.network.ReserveBikeRequest
import com.example.bikey.ui.network.bikeAPI
import com.example.bikey.ui.operator.model.BicycleResponse
import com.example.bikey.ui.operator.model.DockResponse
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(
    station: DockingStationResponse?,
    riderId: String,
    onBack: () -> Unit,
) {
    var reservedBikeId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var availableBikes by remember { mutableStateOf(listOf<BicycleResponse>()) }

    LaunchedEffect(station) {
        availableBikes = station?.docks
            ?.mapNotNull { it.bike }
            ?.filter { bike ->
                val expiryTime = bike.reservationExpiryTime?.takeIf { it.isNotBlank() }?.let { expiryStr ->
                    try {
                        Instant.parse(expiryStr)
                    } catch (_: Exception) { null }
                }
                expiryTime == null || expiryTime.isBefore(Instant.now())
            } ?: emptyList()
    }
    /*val allBikes = remember(station) {
        station?.docks
            ?.mapNotNull { it.bike }
            ?.filter { bike ->
                val expiryTime = bike.reservationExpiryTime?.takeIf { it.isNotBlank() }?.let { expiryStr ->
                    try {
                        Instant.parse(expiryStr)
                    } catch (_: Exception) { null }
                }
                expiryTime == null || expiryTime.isBefore(Instant.now())
            } ?: emptyList()
    }*/

    val regularBikes = availableBikes.filterNot { it.isEBike }
    val eBikes = availableBikes.filter { it.isEBike }

    fun reserveBike(bike: BicycleResponse) {
        scope.launch {
            loading = true
            message = null
            try {
                val response = bikeAPI.reserveBike(
                    ReserveBikeRequest(
                        stationId = station!!.id,
                        bikeId = bike.id,
                        userId = riderId
                    )
                )
                if (response.isSuccessful) {
                    val res = response.body()
                    reservedBikeId = res?.bikeId
                    val expires = res?.reservedUntilEpochMs?.let {
                        Instant.ofEpochMilli(it)
                    }
                    message =
                        "Reserved bike ${bike.id}! Expires at ${expires ?: "soon"}"

                    // Remove reserved bike from displayed list
                    availableBikes = availableBikes.filterNot { it.id == bike.id }
                } else {
                    message = "Failed to reserve (code ${response.code()})"
                }
            } catch (e: Exception) {
                message = "Network error: ${e.message}"
            } finally {
                loading = false
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(station?.name ?: "Station Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (station == null) {
                Text("Loading station details...")
                return@Column
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            message?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Display bikes
            // Display regular bikes
            BikeSection(
                title = "Regular Bikes",
                bikes = regularBikes,
                onReserveBike = ::reserveBike
            )

            // Display e-bikes
            BikeSection(
                title = "E-Bikes",
                bikes = eBikes,
                onReserveBike = ::reserveBike
            )
        }
    }
}
@Composable
fun BikeSection(
    title: String,
    bikes: List<BicycleResponse>,
    onReserveBike: (BicycleResponse) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (bikes.isEmpty()) {
            Text(
                text = "No available bikes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Generate bike numbers per type dynamically
            bikes.forEachIndexed { index, bike ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (bike.isEBike) "E-Bike #${index + 1}" else "Bike #${index + 1}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Status: ${bike.statusTransitions.lastOrNull()?.toState ?: "Available"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = { onReserveBike(bike) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reserve")
                        }
                    }
                }
            }
        }
    }
}