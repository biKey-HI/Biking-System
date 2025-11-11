package com.example.bikey.ui.operator

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bikey.ui.network.mapAPI
import com.example.bikey.ui.operator.model.DockingStationResponse
import com.example.bikey.ui.theme.EcoGreen
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.example.bikey.ui.theme.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorMapDashboardScreen(
    onNavigateBack: () -> Unit
) {
    var stations by remember { mutableStateOf<List<DockingStationResponse>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var selectedStation by remember { mutableStateOf<DockingStationResponse?>(null) }
    var panelExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val response = mapAPI.map()
            if (response.isSuccessful) {
                stations = response.body() ?: emptyList()
            } else {
                errorMsg = "Failed to load stations"
            }
        } catch (ex: Exception) {
            errorMsg = "Network error"
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.5017, -73.5673), 13f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // Use systemBarsPadding for overall safety
    ) {

        // --- Map Layer ---
        if (errorMsg != null) {
            Text(
                text = "⚠️ $errorMsg",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp).align(Alignment.Center)
            )
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = com.google.maps.android.compose.MapUiSettings(
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = true
                )
            ) {
                stations.forEach { station ->
                    Marker(
                        state = MarkerState(position = LatLng(station.location.latitude, station.location.longitude)),
                        title = station.name,
                        snippet = "Bikes: ${station.numOccupiedDocks}/${station.capacity}",
                        onInfoWindowClick = {
                            selectedStation = station
                            panelExpanded = true
                        }
                    )
                }
            }
        }

        //GO BACK BUTTON (Floating on Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .zIndex(10f)
        ) {
            FloatingActionButton(
                onClick = onNavigateBack,
                containerColor = PureWhite,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    tint = EcoGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

            OperatorStationPanel(
                selectedStation = selectedStation,
                isExpanded = panelExpanded,
                onExpandChange = { panelExpanded = it },
                onMoveBike = { sourceStation ->
                    Toast.makeText(context, "🚲 Move bike from ${sourceStation.name}", Toast.LENGTH_SHORT).show()
                    // TODO: open move dialog / call move API
                },
                onMarkMaintenance = { station ->
                    Toast.makeText(context, "🛠 Marked ${station.name} as under maintenance", Toast.LENGTH_SHORT).show()
                    // TODO: update maintenance state via API
                },
                onRestoreSystem = {
                    Toast.makeText(context, "🔄 Restoring initial system state...", Toast.LENGTH_SHORT).show()
                    // TODO: reset API call
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }

}

@Composable
fun OperatorStationPanel(
    selectedStation: DockingStationResponse?,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onMoveBike: (DockingStationResponse) -> Unit,
    onMarkMaintenance: (DockingStationResponse) -> Unit,
    onRestoreSystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelHeight by animateFloatAsState(
        targetValue = if (isExpanded) 0.7f else 0.15f,
        label = "panelHeight"
    )

    Card(
        modifier = modifier
            .fillMaxHeight(panelHeight)
            .shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, drag ->
                    if (drag < -40) onExpandChange(true)
                    else if (drag > 40) onExpandChange(false)
                }
            },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedStation == null) {
                // Centered hint when no station is selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a station to view details",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                //Station header
                Text(
                    text = selectedStation.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${selectedStation.address.line1}, ${selectedStation.address.city}",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OperatorStat("Bikes", selectedStation.numOccupiedDocks.toString())
                    OperatorStat("Free Docks", selectedStation.numFreeDocks.toString())
                    OperatorStat("Capacity", selectedStation.capacity.toString())
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Status Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (selectedStation.status) {
                            "Full" -> Color(0xFFFFEBEE)
                            "Empty" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E9)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = EcoGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Status: ${selectedStation.status}", color = DarkGreen)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Operator actions
                Text(
                    text = "Operator Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Move Bike button — icon + text
                Button(
                    onClick = { onMoveBike(selectedStation) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoGreen)
                ) {
                    Icon(Icons.Filled.Build, contentDescription = "Move bike")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Move Bike")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onMarkMaintenance(selectedStation) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Build, contentDescription = "Mark maintenance")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Maintenance")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onRestoreSystem,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Restore system")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore System State")
                }
            } // end else selectedStation != null
        }
    }
}

@Composable
fun OperatorStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = EcoGreen))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

