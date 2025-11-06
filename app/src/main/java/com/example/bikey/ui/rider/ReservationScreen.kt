package com.example.bikey.ui.rider


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bikey.ui.operator.model.DockingStationResponse
import androidx.compose.material.icons.filled.ArrowBack
import com.example.bikey.ui.network.mapAPI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(
    station: DockingStationResponse?,
    onBack: () -> Unit,
    onConfirmReservation: (String, String) -> Unit = { _, _ -> }
) {
    var pickupLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(station?.name ?: "Loading Station...") },
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Book your next ride with ease",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )



            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    onConfirmReservation(pickupLocation, destination)
                },
                enabled = pickupLocation.isNotBlank() && destination.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Confirm Reservation",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Cancel")
            }
        }
    }
}
