package com.example.bikey.ui.operator

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.bikey.ui.network.MoveBikeRequest
import com.example.bikey.ui.network.ToggleBikeMaintenanceRequest
import com.example.bikey.ui.network.ToggleStationOutOfServiceRequest
import com.example.bikey.ui.network.bikeAPI
import com.example.bikey.ui.network.mapAPI
import com.example.bikey.ui.operator.model.BicycleResponse
import com.example.bikey.ui.operator.model.DockingStationResponse
import com.example.bikey.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorMapDashboardScreen(
    operatorId: String,
    onNavigateBack: () -> Unit
) {
    var stations by remember { mutableStateOf<List<DockingStationResponse>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var selectedStation by remember { mutableStateOf<DockingStationResponse?>(null) }
    var panelExpanded by remember { mutableStateOf(false) }
    var showMoveBikeDialog by remember { mutableStateOf(false) }
    var bikeToMove by remember { mutableStateOf<BicycleResponse?>(null) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var bikeToToggleMaintenance by remember { mutableStateOf<BicycleResponse?>(null) }


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
    suspend fun refreshStationsAndSyncSelection() {
        val refreshed = mapAPI.map()
        if (refreshed.isSuccessful) {
            val newStations = refreshed.body() ?: stations
            stations = newStations
            selectedStation?.let { old ->
                selectedStation = newStations.find { it.id == old.id }
            }
        }
    }

    // --- Operator Actions ---

    fun moveBike(bike: BicycleResponse,from: DockingStationResponse, to: DockingStationResponse) {
        scope.launch {
            try {
                val freeDockId = to.docks
                    .firstOrNull { it.bike == null && it.status.equals("Empty", ignoreCase = true) }
                    ?.id

                if (freeDockId == null) {
                    Toast.makeText(context, "No free docks available at ${to.name}", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val req = MoveBikeRequest(
                    fromStationId = from.id,
                    userId = operatorId,
                    bikeId = bike.id,
                    toDockId = freeDockId,
                    toStationId = to.id,
                )
                val res = bikeAPI.moveBike(req)
                if (res.isSuccessful) {
                    Toast.makeText(context, "Moved bike ${bike.id} to ${to.name}", Toast.LENGTH_SHORT).show()
                    // Refresh stations after move
                    refreshStationsAndSyncSelection()
                } else {
                    Toast.makeText(context, "Move failed (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleBikeMaintenance(station: DockingStationResponse, bike: BicycleResponse) {
        scope.launch {
            try {
                val req = ToggleBikeMaintenanceRequest(
                    dockingStationId = station.id,
                    userId = operatorId,
                    bikeId = bike.id
                )
                val res = bikeAPI.toggleBikeMaintenance(req)
                if (res.isSuccessful) {
                    Toast.makeText(context, "Bike ${bike.id} maintenance toggled", Toast.LENGTH_SHORT).show()
                    refreshStationsAndSyncSelection()
                } else {
                    Toast.makeText(context, "Maintenance failed (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun toggleStationOutOfService(station: DockingStationResponse) {
        scope.launch {
            try {
                val req = ToggleStationOutOfServiceRequest(
                    dockingStationId = station.id,
                    userId = operatorId
                )
                val res = bikeAPI.toggleStationOutOfService(req)
                if (res.isSuccessful) {
                    Toast.makeText(context, "Station out-of-service toggled", Toast.LENGTH_SHORT).show()
                    refreshStationsAndSyncSelection()
                } else {
                    Toast.makeText(context, "Toggle failed (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun restoreSystem() {
        scope.launch {
            try {
                val refreshed = mapAPI.map()
                if (refreshed.isSuccessful) {
                    stations = refreshed.body() ?: stations
                    Toast.makeText(context, "System refreshed", Toast.LENGTH_SHORT).show()
                    refreshStationsAndSyncSelection()
                } else {
                    Toast.makeText(context, "Failed to refresh (${refreshed.code()})", Toast.LENGTH_SHORT).show()
                }
                // you can re-fetch all stations or reset them in backend
                //stations = bikeAPI.getAllStations().body().orEmpty()
            } catch (e: Exception) {
                Toast.makeText(context, "⚠️ Failed to restore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
                            panelExpanded = true
                            scope.launch {
                                val refreshed = mapAPI.map()
                                if (refreshed.isSuccessful) {
                                    val newStations = refreshed.body() ?: stations
                                    stations = newStations
                                    selectedStation = newStations.find { it.id == station.id }
                                } else {
                                    selectedStation = station
                                }
                            }
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
            onMoveBike = { station ->
                scope.launch {
                    refreshStationsAndSyncSelection()
                    selectedStation = stations.find { it.id == station.id }
                    showMoveBikeDialog = true
                }
            },
            onMarkMaintenance = { station ->
                scope.launch {
                    refreshStationsAndSyncSelection()
                    selectedStation = stations.find { it.id == station.id }
                    showMaintenanceDialog = true
                }
            },
            onToggleOutOfService = { station -> toggleStationOutOfService(station) },
            onRestoreSystem = { restoreSystem() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        if (showMoveBikeDialog && selectedStation != null) {
            LaunchedEffect(selectedStation) {
                refreshStationsAndSyncSelection()
            }
            SelectBikeDialog(
                station = selectedStation!!,
                onBikeSelected = { selected ->
                    bikeToMove = selected
                    showMoveBikeDialog = false // step to next
                },
                onDismiss = { showMoveBikeDialog = false }
            )
        }
        if (!showMoveBikeDialog && bikeToMove != null && selectedStation != null) {
            LaunchedEffect(selectedStation) {
                refreshStationsAndSyncSelection()
            }
            SelectDestinationStationDialog(
                originStation = selectedStation!!,
                allStations = stations.filter { it.id != selectedStation!!.id },
                bikeToMove = bikeToMove!!,
                onMoveConfirmed = { dest ->
                    moveBike(bikeToMove!!, selectedStation!!, dest)
                    // reset flow after move
                    bikeToMove = null
                    panelExpanded = false
                },
                onDismiss = { bikeToMove = null }
            )
        }
        if (showMaintenanceDialog && selectedStation != null) {
            LaunchedEffect(selectedStation) {
                refreshStationsAndSyncSelection()
            }
            SelectBikeDialog(
                station = selectedStation!!,
                onBikeSelected = { selected ->
                    bikeToToggleMaintenance = selected
                    showMaintenanceDialog = false
                    toggleBikeMaintenance(selectedStation!!, selected)
                    bikeToToggleMaintenance = null
                    panelExpanded = false
                },
                onDismiss = { showMaintenanceDialog = false }
            )
        }


    }

}

@Composable
fun OperatorStationPanel(
    selectedStation: DockingStationResponse?,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onMoveBike: (DockingStationResponse) -> Unit,
    onMarkMaintenance: (DockingStationResponse) -> Unit,
    onToggleOutOfService: (DockingStationResponse) -> Unit,
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
                    Text("Toggle Bike Maintenance")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onToggleOutOfService(selectedStation) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Build, contentDescription = "Toggle out of service")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Toggle Station Out-of-Service")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectBikeDialog(
    station: DockingStationResponse,
    onBikeSelected: (BicycleResponse) -> Unit,
    onDismiss: () -> Unit
) {
    // collect available bikes from docks
    val availableBikes = station.docks.mapNotNull { it.bike }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Select Bike to Move (Station: ${station.name})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (availableBikes.isEmpty()) {
                    Text("No bikes available at this station.", color = Color.Gray)
                } else {
                    availableBikes.forEachIndexed { index, bike ->
                        ListItem(
                            headlineContent = { Text("Bike ${index + 1}") },
                            supportingContent = { Text(if (bike.isEBike) "E-Bike" else "Classic Bike") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.DirectionsBike,
                                    contentDescription = null,
                                    tint = EcoGreen
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBikeSelected(bike) }
                        )
                        Divider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Cancel")
                }
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(24.dp)
    )
}
// New Composable for selecting the Destination Station
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectDestinationStationDialog(
    originStation: DockingStationResponse,
    allStations: List<DockingStationResponse>,
    bikeToMove: BicycleResponse,
    onMoveConfirmed: (DockingStationResponse) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter stations based on search query (reusing rider's search logic)
    val filteredStations = remember(allStations, searchQuery) {
        if (searchQuery.isBlank()) {
            allStations
        } else {
            allStations.filter { station ->
                station.name.contains(searchQuery, ignoreCase = true) ||
                        station.address.line1.contains(searchQuery, ignoreCase = true) ||
                        station.address.city.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Move Bike (from ${originStation.name})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select Destination Station:")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search station by name or address") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = EcoGreen)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EcoGreen,
                        unfocusedBorderColor = Color.LightGray,
                        cursorColor = EcoGreen
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredStations.isEmpty()) {
                    Text("No stations match your search.", color = Color.Gray)
                } else {
                    filteredStations.forEach { station ->
                        val selectable = station.numFreeDocks > 0
                        ListItem(
                            headlineContent = { Text(station.name) },
                            supportingContent = {
                                Text(
                                    "${station.address.city} | Free Docks: ${station.numFreeDocks}",
                                    color = if (selectable) EcoGreen else Color.Red
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (selectable) EcoGreen else Color.LightGray
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = selectable) { onMoveConfirmed(station) }
                        )
                        Divider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Cancel Move")
                }
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(24.dp)
    )
}