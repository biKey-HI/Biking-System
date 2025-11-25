package com.example.bikey.ui.rider

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.bikey.ui.network.mapAPI
import com.example.bikey.ui.network.bikeAPI
import com.example.bikey.ui.network.LoyaltyTierResponse
import com.example.bikey.ui.operator.model.DockingStationResponse
import com.example.bikey.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.CheckCircle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalContext
import com.example.bikey.ui.network.TakeBikeRequest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.produceState
import com.example.bikey.ui.network.ReturnAndSummaryResponse
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.HorizontalDivider
import com.example.bikey.ui.PricingPlan
import com.example.bikey.ui.UserContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.bikey.ui.network.RideHistoryItemDTO 
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.example.bikey.ui.ViewMode
import androidx.compose.ui.text.style.TextAlign
import com.example.bikey.ui.network.directionsApi


data class ActiveRideInfo(
    val bikeId: String,
    val tripId: String,
    val startedAtMs: Long,
    val origStationId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboardScreen(
    riderEmail: String,
    onLogout: () -> Unit,
    onReserveBike: (DockingStationResponse) -> Unit
) {
    var showTripSummary by remember { mutableStateOf<ReturnAndSummaryResponse?>(null) }

    // for debug
    val context = LocalContext.current

    val username = riderEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
    var stations by remember { mutableStateOf<List<DockingStationResponse>>(emptyList()) }
    var selectedStation by remember { mutableStateOf<DockingStationResponse?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var panelExpanded by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(BikeFilter.ALL) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showLoyaltyProgress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bikeApi = bikeAPI
    var lastTripDistance by remember {mutableStateOf<Int?>(null)}
    var lastReturnStation by remember {mutableStateOf<DockingStationResponse?>(null)}


    var activeRide by remember { mutableStateOf<ActiveRideInfo?>(null) }
    val hasActiveRide = activeRide != null
    val activeRideStartMs = activeRide?.startedAtMs

    // Take bike function
    fun onTakeBike(station: DockingStationResponse) {
        Log.d("TakeBike", "onTakeBike() called")
        if (hasActiveRide || station.numOccupiedDocks <= 0)
            return

        // ui update
        val prev = stations
        stations = stations.map {
            if (it.id == station.id) it.copy(
                numOccupiedDocks = it.numOccupiedDocks - 1,
                numFreeDocks = it.numFreeDocks + 1
            ) else it
        }

        if(selectedStation?.id == station.id) selectedStation = station

        scope.launch {
            try {
                Log.d("TakeBike", "sending request… id=${station.id}") // debug
                val res = bikeApi.takeBike(TakeBikeRequest(stationId = station.id, userEmail = riderEmail))
                Log.d("TakeBike", "response: code=${res.code()} msg=${res.message()} bodyNull=${res.body()==null}") // debug
                val body = if (res.isSuccessful) res.body() else null
                if (body != null) {
                    activeRide = ActiveRideInfo(
                        bikeId = body.bikeId,
                        tripId = body.tripId,
                        startedAtMs = body.startedAtEpochMs,
                        origStationId = station.id
                    )
                    Toast.makeText(context, "Trip started ", Toast.LENGTH_SHORT).show() // debug
                } else {
                    stations = prev // on failure
                    Toast.makeText(context, "Couldn’t start trip (${res.code()})", Toast.LENGTH_SHORT).show() // debug

                }
            } catch (_: Exception) {
                stations = prev // on error
            }
        }
    }


    // Return bike function
    fun onReturnBike(station: DockingStationResponse) {
        Log.d("ReturnBike", "onReturnBike() called")
        Log.d("ReturnBike", "hasActiveRide: $hasActiveRide, stationFreeDocks: ${station.numFreeDocks}")
        if (!hasActiveRide || station.numFreeDocks <= 0)
            return

        scope.launch {
            try {
                Log.d("origStationId", activeRide?.origStationId ?: "null")
                val origStation = stations.find {it.id == activeRide?.origStationId}
                origStation?.let{
                    lastTripDistance = getRouteDistance(origStation, station)
                    Log.d("Distance", if(lastTripDistance == null) "null" else "${lastTripDistance ?: 0}")
                    UserContext.kilometersTravelled += lastTripDistance ?: 0
                } ?: return@launch

                val activeTripId = activeRide?.tripId ?: return@launch
                Log.d("ReturnBike", "Active trip ID: $activeTripId")
                Log.d("ReturnBike", "Destination station ID: ${station.id}")
                // val bikeId = activeRide?.tripId: return@launch
                val returnRequest = com.example.bikey.ui.network.ReturnBikeRequest(
                    tripId = activeTripId,
                    destStationId = station.id,
                    dockId = null, // auto-assign dock
                    distanceTravelled = lastTripDistance ?: 0
                )

                Log.d("ReturnBike", "Sending return request...")
                val res = bikeApi.returnBike(returnRequest)
                Log.d("ReturnBike", "Response code: ${res.code()}")
                Log.d("ReturnBike", "Response message: ${res.message()}")
                Log.d("ReturnBike", "Response isSuccessful: ${res.isSuccessful}")
                if (res.isSuccessful) {
                    val response = res.body()

                    // Show summary to user
//                    response?.summary?.let { summary ->
//                        Log.d("TripSummary", "Cost: $${summary.cost.totalCents / 100.0}")
//                        Toast.makeText(context, "Trip completed! Cost: $${summary.cost.totalCents / 100.0}", Toast.LENGTH_LONG).show()
//                    }
                    activeRide = null
                    lastReturnStation = station

                    response?.let {
                        showTripSummary = it
                    }



                    Toast.makeText(context, "Bike returned successfully! Any payments have been processed.", Toast.LENGTH_SHORT).show()

                    // refresh stations
                    val stationsResponse = mapAPI.map()
                    if (stationsResponse.isSuccessful) {
                        stations = stationsResponse.body() ?: emptyList()
                        selectedStation = stations.firstOrNull { it.id == selectedStation?.id } ?: selectedStation
                        UserContext.user?.flexDollars -= (showTripSummary?.summary?.cost?.flexDollarCents ?: 0).toFloat()/100f
                        UserContext.user?.flexDollars += if((lastReturnStation!!.numOccupiedDocks.toFloat())/(lastReturnStation!!.numOccupiedDocks + lastReturnStation!!.numFreeDocks).toFloat() < 0.25f) 0.25f else 0f
                    }
                } else {
                    val errorBody = res.errorBody()?.string()
                    Log.e("ReturnBike", "Failed response - Code: ${res.code()}, Error: $errorBody")
                    Toast.makeText(context, "Failed to return bike", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ReturnBike", "Error returning bike", e)
                Toast.makeText(context, "Error returning bike", Toast.LENGTH_SHORT).show()
            }
        }
    }



    // Load stations
    LaunchedEffect(Unit) {
        try {
            val response = mapAPI.map()
            if (response.isSuccessful) {
                stations = response.body() ?: emptyList()
            }
        } catch (_: Exception) {
            // Handle error silently for now
        }
    }

    // Filter stations based on selected filter
    val filteredStations = remember(stations, selectedFilter) {
        when (selectedFilter) {
            BikeFilter.ALL -> stations
            BikeFilter.EBIKES -> stations.filter { station ->
                station.docks.any { dock ->
                    dock.bike != null && dock.bike.isEBike
                }
            }
            BikeFilter.CLASSIC -> stations.filter { station ->
                station.docks.any { dock ->
                    dock.bike != null && !dock.bike.isEBike
                }
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.5017, -73.5673), 13f)
    }

    if (showTripSummary != null && lastReturnStation != null) {
            TripSummaryScreen(
                summary = showTripSummary!!,
                offersFlexDollars = lastReturnStation?.let{(lastReturnStation!!.numOccupiedDocks.toFloat())/(lastReturnStation!!.numOccupiedDocks + lastReturnStation!!.numFreeDocks).toFloat() < 0.25f} ?: false,
                onDone = {
                    showTripSummary = null
                },
                distanceTravelled = lastTripDistance ?: 0
            )
    } else {
        Box(
            modifier = Modifier
        .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Map Layer
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = com.google.maps.android.compose.MapUiSettings(
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = true
                )
            ) {
                filteredStations.forEach { station ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(
                                station.location.latitude,
                                station.location.longitude
                            )
                        ),
                        title = station.name,
                        snippet = "Available bikes: ${station.numOccupiedDocks}",
                        onInfoWindowClick = {
                            selectedStation = station
                            panelExpanded = true
                        }
                    )
                }
            }

            // Filter Button (Top Center)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(10f)
            ) {
                FloatingActionButton(
                    onClick = { showFilterMenu = !showFilterMenu },
                    containerColor = PureWhite,
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 150.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Filter",
                            tint = EcoGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedFilter.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DarkGreen
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showFilterMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = EcoGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Filter Dropdown Menu
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier
                        .background(PureWhite)
                        .widthIn(min = 180.dp)
                ) {
                    BikeFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = when (filter) {
                                            BikeFilter.ALL -> Icons.Default.Star
                                            BikeFilter.EBIKES -> Icons.Default.Build
                                            BikeFilter.CLASSIC -> Icons.Default.Favorite
                                        },
                                        contentDescription = null,
                                        tint = if (selectedFilter == filter) EcoGreen else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = filter.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedFilter == filter) DarkGreen else Color.DarkGray
                                        )
                                    )
                                    if (selectedFilter == filter) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = EcoGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedFilter = filter
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }

            // Hamburger Menu Button (Top Left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .zIndex(10f)
            ) {
                FloatingActionButton(
                    onClick = { showMenu = true },
                    containerColor = PureWhite,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = EcoGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Search Button (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .zIndex(10f)
            ) {
                FloatingActionButton(
                    onClick = { showSearchDialog = true },
                    containerColor = PureWhite,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = EcoGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                if (showSearchDialog) {
                    SearchStationDialog(
                        stations = stations,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onStationSelected = { station ->
                            selectedStation = station
                            panelExpanded = true
                            showSearchDialog = false
                            searchQuery = ""
                            // Optionally move camera to station
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(station.location.latitude, station.location.longitude),
                                15f
                            )
                        },
                        onDismiss = {
                            showSearchDialog = false
                            searchQuery = ""
                        }
                    )
                }

            }

            // Slide-up Panel
            SlideUpPanel(
                username = username,
                selectedStation = selectedStation,
                isExpanded = panelExpanded,
                onExpandChange = { panelExpanded = it },
                hasActiveRide = hasActiveRide,
                activeRideStartMs = activeRideStartMs,
                onTakeBike = { st -> onTakeBike(st) },
                onReturnBike = { st -> onReturnBike(st) },
                onReserveBike = { station ->
                onReserveBike(station)
            },
            modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )

            // Hamburger Menu Drawer
            if (showMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                ) {
                    HamburgerMenu(
                        username = username,
                        onDismiss = { showMenu = false },
                        onLogout = {
                            showMenu = false
                            onLogout()
                        },
                        
                    )
                }
            }
        }
    }
}

suspend fun getRouteDistance(origin: DockingStationResponse, destination: DockingStationResponse): Int {
    val origin = "${origin.location.latitude},${origin.location.longitude}"
    val destination = "${destination.location.latitude},${destination.location.longitude}"
    val res = directionsApi.getDistance(origin, destination)
    return (res.routes.firstOrNull()?.legs?.firstOrNull()?.distance?.value ?: 0)/1000
}

@Composable
fun SlideUpPanel(
    username: String,
    selectedStation: DockingStationResponse?,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onReserveBike: (DockingStationResponse) -> Unit,
    hasActiveRide: Boolean,
    activeRideStartMs: Long?,
    onTakeBike: (DockingStationResponse) -> Unit,
    onReturnBike: (DockingStationResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val panelHeight by animateFloatAsState(
        targetValue = if (isExpanded) 0.8f else 0.15f,
        label = "panelHeight"
    )

    Card(
        modifier = modifier
            .fillMaxHeight(panelHeight)
            .shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -50) onExpandChange(true)
                    else if (dragAmount > 50) onExpandChange(false)
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
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
                    .align(Alignment.CenterHorizontally)
                    .clickable { onExpandChange(!isExpanded) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isExpanded) {
                // Collapsed State - Welcome Message
                Text(
                    text = "Hi, $username!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap a station marker to see bike availability",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                // Expanded State - Station Details
                if (selectedStation != null) {
                    StationDetails(
                        station = selectedStation,
                        hasActiveRide = hasActiveRide,
                        activeRideStartMs = activeRideStartMs,
                        onTakeBike = onTakeBike,
                        onReturnBike = onReturnBike,
                        onReserveBike = { station -> onReserveBike(station) }
                    )
                } else {
                    Text(
                        text = "Hi, $username!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select a station on the map to view details and available bikes.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun StationDetails(
    
    station: DockingStationResponse,
    onReserveBike: (DockingStationResponse) -> Unit,
    hasActiveRide: Boolean,
    activeRideStartMs: Long?,
    onTakeBike: (DockingStationResponse) -> Unit,
    onReturnBike: (DockingStationResponse) -> Unit
) {

    // for debug
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${station.address.line1}, ${station.address.city}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // active ride banner with timer
        if (hasActiveRide && activeRideStartMs != null) {
            val elapsedSeconds by produceState(0L) {
                while (true) {
                    value = ((System.currentTimeMillis() - activeRideStartMs!!) / 1000)
                    kotlinx.coroutines.delay(1000)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = EcoGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Trip in progress",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = DarkGreen
                        )
                        Text(
                            text = "Elapsed time: ${elapsedSeconds / 60}m ${elapsedSeconds % 60}s",
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Station Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StationStat(
                icon = Icons.Default.Star,
                label = "Available Bikes",
                value = station.numOccupiedDocks.toString(),
                color = EcoGreen
            )
            StationStat(
                icon = Icons.Default.LocationOn,
                label = "Free Docks",
                value = station.numFreeDocks.toString(),
                color = DarkGreen
            )
            StationStat(
                icon = Icons.Default.Build,
                label = "Capacity",
                value = station.capacity.toString(),
                color = DarkGreen
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Badge
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (station.status) {
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
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = when (station.status) {
                        "Full" -> Color(0xFFD32F2F)
                        "Empty" -> Color(0xFFF57C00)
                        else -> EcoGreen
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Status: ${station.status}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = when (station.status) {
                        "Full" -> Color(0xFFD32F2F)
                        "Empty" -> Color(0xFFF57C00)
                        else -> EcoGreen
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Take Bike
            Button(
                // debug
                onClick = { Log.d("TakeBike", "BUTTON onClick for station=${station.name}") // debug
                    Toast.makeText(context, "Take Bike pressed", Toast.LENGTH_SHORT).show() // debug
                    UserContext.user?.hasReservation = false; UserContext.user?.reservationStationId = null; onTakeBike(station) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                shape = RoundedCornerShape(16.dp),
                // temporarily force-enabled to rule out disabled state
                // enabled = true //  debugging
                enabled = station.numOccupiedDocks > 0
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (station.numOccupiedDocks > 0) "Take a Bike" else "No Bikes Available",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
            // Return Bike
            Button(
                onClick = { Log.d("ReturnBike", "Return Bike pressed for station=${station.name}")
                    onReturnBike(station)  },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                shape = RoundedCornerShape(16.dp),
                enabled = hasActiveRide && station.numFreeDocks > 0
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (station.numFreeDocks > 0) "Return a Bike" else "No Docks Available",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
            // Reserve a bike
            Button(
                onClick = { UserContext.user?.hasReservation = true; UserContext.user?.reservationStationId = station.id; onReserveBike(station) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                shape = RoundedCornerShape(16.dp),
                enabled = station.numOccupiedDocks > 0 && !station.aBikeIsReserved && !(UserContext.hasReservation ?: false)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (station.numOccupiedDocks > 0 && !station.aBikeIsReserved && !(UserContext.hasReservation ?: false)) "Reserve a Bike" else "No Bikes Available",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
fun StationStat(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HamburgerMenu(
    username: String,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    var loyaltyTier by remember { mutableStateOf<LoyaltyTierResponse?>(null) }
    var isLoadingLoyalty by remember { mutableStateOf(true) }
    var showLoyaltyProgress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val userId = UserContext.user?.id.toString()

    // Fetch loyalty tier when menu opens
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = bikeAPI.getLoyaltyTier(userId)
                if (response.isSuccessful) {
                    loyaltyTier = response.body()
                }
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                isLoadingLoyalty = false
            }
        }
    }

    // Show loyalty progress dialog
    if (showLoyaltyProgress) {
        LoyaltyProgressDialog(
            userId = userId,
            onDismiss = { showLoyaltyProgress = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )

        // Drawer Sheet on top
        ModalDrawerSheet(
            drawerContainerColor = PureWhite,
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EcoGreen)
                        .padding(24.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Rider Account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite.copy(alpha = 0.6f)
                            )
                            Text(
                                text = UserContext.pricingPlan?.displayName ?: "No Plan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite.copy(alpha = 0.8f)
                            )
                        }

                        // Loyalty Tier Badge
                        if (!isLoadingLoyalty && loyaltyTier != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val tierColor = when (loyaltyTier?.tier) {
                                "BRONZE" -> Color(0xFFCD7F32)
                                "SILVER" -> Color(0xFFC0C0C0)
                                "GOLD" -> Color(0xFFFFD700)
                                else -> Color.Gray.copy(alpha = 0.3f)
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = tierColor,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = loyaltyTier?.tierDisplayName ?: "No Tier",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = PureWhite
                                            )
                                        )
                                        if ((loyaltyTier?.discountPercentage ?: 0f) > 0) {
                                            Text(
                                                text = "${(loyaltyTier!!.discountPercentage * 100).toInt()}% Discount",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PureWhite.copy(alpha = 0.9f)
                                            )
                                        }
                                        if ((loyaltyTier?.reservationHoldExtraMinutes ?: 0) > 0) {
                                            Text(
                                                text = "+${loyaltyTier!!.reservationHoldExtraMinutes} min hold time",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PureWhite.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = "Distance Travelled: ${UserContext.kilometersTravelled} km",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PureWhite.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "CO2 Emissions Avoided: ${(UserContext.kilometersTravelled*0.25).toInt()} kg",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PureWhite.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Flex Dollars: $${"%.2f".format(UserContext.flexDollars)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PureWhite.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                }

                Spacer(modifier = Modifier.height(16.dp))

                // Loyalty Rewards Section (if Bronze tier)
                if (loyaltyTier?.tier == "BRONZE") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF8E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFCD7F32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Bronze Benefits",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6D4C41)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✓ 5% discount on all trips",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6D4C41)
                            )
                            Text(
                                text = "✓ Loyal rider recognition",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6D4C41)
                            )
                            Text(
                                text = "✓ Priority support",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6D4C41)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                MenuItemButton(
                    icon = Icons.Filled.CheckCircle,
                    text = "Pricing Plans",
                    onClick = { UserContext.nav?.navigate("selectPricing")}
                )

                // Add Loyalty Rewards menu item
                MenuItemButton(
                    icon = Icons.Default.Star,
                    text = "Loyalty Progress",
                    onClick = {
                        showLoyaltyProgress = true
                    },
                    badge = if (loyaltyTier?.tier == "BRONZE" || loyaltyTier?.tier == "SILVER" || loyaltyTier?.tier == "GOLD") loyaltyTier?.tier else null
                )

                MenuItemButton(
                    icon = Icons.Default.AccountBox,
                    text = "Payment Methods",
                    onClick = { /* TODO */ }
                )

                MenuItemButton(
                    icon = Icons.Default.DateRange,
                    text = "Ride History",
                    onClick = {
                        onDismiss()
                        UserContext.nav?.navigate("rideHistory")
                    }
                )

                MenuItemButton(
                    icon = Icons.Default.Favorite,
                    text = "Saved Stations",
                    onClick = { /* TODO */ }
                )

                MenuItemButton(
                    icon = Icons.Default.Notifications,
                    text = "Notifications",
                    onClick = { /* TODO */ }
                )

                MenuItemButton(
                    icon = Icons.Default.Settings,
                    text = "Settings",
                    onClick = { /* TODO */ }
                )

                MenuItemButton(
                    icon = Icons.Default.Info,
                    text = "Help & Support",
                    onClick = { /* TODO */ }
                )

                Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

                MenuItemButton(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    text = "Logout",
                    onClick = onLogout,
                    textColor = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MenuItemButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: Color = DarkGreen,
    badge: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            )

            // Badge display
            if (badge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFCD7F32),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = Color.LightGray
            )
        }
    }
}

enum class BikeFilter(val displayName: String) {
    ALL("All Bikes"),
    EBIKES("E-Bikes"),
    CLASSIC("Classic Bikes")
}


//Functions to help the search box find the correct station
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchStationDialog(
    stations: List<DockingStationResponse>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStationSelected: (DockingStationResponse) -> Unit,
    onDismiss: () -> Unit
) {
    // Filter stations based on search query
    val filteredStations = remember(stations, searchQuery) {
        if (searchQuery.isBlank()) {
            stations
        } else {
            stations.filter { station ->
                station.name.contains(searchQuery, ignoreCase = true) ||
                        station.address.line1.contains(searchQuery, ignoreCase = true) ||
                        station.address.city.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search Stations",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name or address") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = EcoGreen
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
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

                // Results Count
                Text(
                    text = "${filteredStations.size} station${if (filteredStations.size != 1) "s" else ""} found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                // Results List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (filteredStations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No stations found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        filteredStations.forEach { station ->
                            StationSearchItem(
                                station = station,
                                onClick = { onStationSelected(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationSearchItem(
    station: DockingStationResponse,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EcoGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = EcoGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Station Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreen
                    )
                )
                Text(
                    text = "${station.address.line1}, ${station.address.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${station.numOccupiedDocks} bike${if (station.numOccupiedDocks != 1) "s" else ""} available",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (station.numOccupiedDocks > 0) EcoGreen else Color.Gray
                    )
                )
            }

            // Arrow Icon
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Select",
                tint = Color.LightGray
            )
        }
    }
    HorizontalDivider()
}


@Composable
fun TripSummaryScreen(
    summary: ReturnAndSummaryResponse,
    onDone: () -> Unit,
    offersFlexDollars: Boolean = false,
    distanceTravelled: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = EcoGreen,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Trip Completed!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Trip Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                TripDetailItem("Start", summary.summary.startStationName)
                TripDetailItem("End", summary.summary.endStationName)
                TripDetailItem("Duration", "${summary.summary.durationMinutes} min")
                TripDetailItem("Distance", "$distanceTravelled km")
                TripDetailItem("Carbon Emissions Avoided", "${distanceTravelled.times(0.25)} kg")
                TripDetailItem("Bike Type", if (summary.summary.isEBike) "E-Bike" else "Classic Bike")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Cost Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {

                Text(
                    text = "Cost Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(16.dp))
                with(summary.summary.cost) {
                    if(UserContext.pricingPlan == PricingPlan.DEFAULT_PAY_NOW) {CostItem("Base fare", baseCents)}
                    eBikeSurchargeCents?.let { CostItem("Electricity rate ($minutes mins)", it) }
                    overtimeCents?.let { if(overtimeCents != 0) CostItem("Overtime charges", it) }

                    // Show loyalty discount if applicable
                    if (discountCents > 0) {
                        CostItem("Loyalty Discount ($loyaltyTier)", -discountCents, isDiscount = true)
                    }

                    if(flexDollarCents > 0) CostItem("Flex Dollars Used", -1*flexDollarCents)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
                    CostItem("Total", totalCents)
                    if(offersFlexDollars) CostItem("Flex Dollars Added to Account", 25)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Done Button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
            )
        }
    }
}

@Composable
private fun TripDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = DarkGreen
        )
    }
}

@Composable
private fun CostItem(label: String, cents: Int, isTotal: Boolean = false, isDiscount: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isTotal) DarkGreen else if (isDiscount) Color.Red else Color.Gray
        )
        Text(
            text = "$${cents / 100.0}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isTotal) DarkGreen else if (isDiscount) Color.Red else Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rides by remember { mutableStateOf<List<RideHistoryItemDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val userId = UserContext.id?.toString()
        if (userId == null) {
            errorMessage = "User not logged in"
            isLoading = false
            return@LaunchedEffect
        }

        scope.launch {
            try {
                isLoading = true
                val response = bikeAPI.getRideHistory(userId)
                if (response.isSuccessful) {
                    rides = response.body() ?: emptyList()
                    errorMessage = null
                } else {
                    errorMessage = "Failed to load ride history: ${response.code()}"
                    Log.e("RideHistory", "Failed to load: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                errorMessage = "Error loading ride history: ${e.message}"
                Log.e("RideHistory", "Error loading ride history", e)
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ride History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EcoGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .systemBarsPadding()
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EcoGreen)
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = EcoGreen)
                        ) {
                            Text("Go Back", color = PureWhite)
                        }
                    }
                }
                rides.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No rides yet",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your completed trips will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rides) { ride ->
                            RideHistoryItem(ride = ride)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RideHistoryItem(ride: RideHistoryItemDTO) {
    // State to manage expansion
    var isExpanded by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
    
    val startTime = try {
        Instant.parse(ride.summary.startTime)
    } catch (e: Exception) {
        null
    }

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with date and cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    startTime?.let {
                        Text(
                            text = dateFormatter.format(it),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DarkGreen
                            )
                        )
                    }
                    Text(
                        text = if (ride.summary.isEBike) "E-Bike" else "Classic Bike",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                // Show plan name if subscription and cost is $0, otherwise show cost
                val isSubscription = ride.paymentStrategy != PricingPlan.DEFAULT_PAY_NOW.displayName
                val costText = if (isSubscription && ride.summary.cost.totalCents == 0) {
                    ride.paymentStrategy
                } else {
                    "$${ride.summary.cost.totalCents / 100.0}"
                }
                Text(
                    text = costText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EcoGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // Route information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = EcoGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ride.summary.startStationName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = DarkGreen
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(vertical = 4.dp)
                    )
                    Text(
                        text = ride.summary.endStationName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = DarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration and details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⏱ ${ride.summary.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (ride.summary.cost.overtimeCents != null && ride.summary.cost.overtimeCents > 0) {
                    Text(
                        text = "Overtime: $${ride.summary.cost.overtimeCents / 100.0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF6B6B)
                    )
                }
            }
            // Expandable Bill Section Toggle
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trip ID: ${ride.summary.tripId}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray
                    )
                )

                Text(
                    text = "Show Bill ${if (isExpanded) "▲" else "▼"}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = EcoGreen,
                        fontWeight = FontWeight.SemiBold
                    ),
                    // Click handler is on the main Card
                )
            }

            // Detailed Bill Content (conditionally shown)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = LightGray)
                Spacer(modifier = Modifier.height(12.dp))

                // --- COST BREAKDOWN ---
                Text(
                    text = "Cost Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                with(ride.summary.cost) {
                    val isPayAsYouGo = ride.paymentStrategy == PricingPlan.DEFAULT_PAY_NOW.displayName
                    val isSubscription = !isPayAsYouGo

                    if (isPayAsYouGo && baseCents > 0) {
                        CostItem("Base fare (Unlock fee)", baseCents)
                    }

                    // We rely on the cents being non-zero to show the line item
                    eBikeSurchargeCents?.let { if (it > 0) CostItem("E-Bike Surcharge", it) }
                    overtimeCents?.let { if (it > 0) CostItem("Overtime charges", it) }

                    // Show loyalty discount if applicable
                    if (discountCents > 0) {
                        CostItem("Loyalty Discount ($loyaltyTier)", -discountCents, isDiscount = true)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
                    // Show plan name if subscription and cost is $0, otherwise show cost
                    if (isSubscription && totalCents == 0) {
                        TripDetailItem("Total Charged", ride.paymentStrategy)
                    } else {
                        CostItem("Total Charged", totalCents, isTotal = true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- PAYMENT INFORMATION ---
                Text(
                    text = "Payment Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Always show the payment plan/strategy
                val isSubscription = ride.paymentStrategy != PricingPlan.DEFAULT_PAY_NOW.displayName
                TripDetailItem(
                    label = if (isSubscription) "Subscription Plan" else "Payment Plan",
                    value = ride.paymentStrategy
                )

                // Show cardholder name if available
                if (ride.cardHolderName != null) {
                    TripDetailItem("Cardholder Name", ride.cardHolderName)
                }

                // Show card last 4 digits if available
                if (ride.savedCardLast4 != null) {
                    val cardProvider = ride.provider ?: "Card"
                    TripDetailItem("Card Used", "$cardProvider ending in •••• ${ride.savedCardLast4}")
                }

                // Payment status summary
                val paymentStatus = when {
                    isSubscription -> "Covered by subscription"
                    ride.summary.cost.totalCents == 0 -> "No charge"
                    ride.hasSavedCard -> "Paid via card on file"
                    else -> "Payment processed"
                }
                TripDetailItem("Payment Status", paymentStatus)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyRewardsScreen(
    onBack: () -> Unit
) {
    var loyaltyTier by remember { mutableStateOf<LoyaltyTierResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Fetch loyalty tier data
    LaunchedEffect(Unit) {
        try {
            val response = bikeAPI.getLoyaltyTier(UserContext.id.toString())
            if (response.isSuccessful) {
                loyaltyTier = response.body()
            } else {
                errorMessage = "Failed to load loyalty data"
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loyalty Rewards", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EcoGreen
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EcoGreen)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Current Tier Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val tierColor = when (loyaltyTier?.tier) {
                            "BRONZE" -> Color(0xFFCD7F32)
                            "SILVER" -> Color(0xFFC0C0C0)
                            "GOLD" -> Color(0xFFFFD700)
                            else -> Color.Gray
                        }
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = loyaltyTier?.tierDisplayName ?: "No Tier",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if ((loyaltyTier?.discountPercentage ?: 0f) > 0) {
                            Text(
                                text = "🎉 ${(loyaltyTier!!.discountPercentage * 100).toInt()}% Discount on All Rides!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoGreen
                                )
                            )
                        }
                        if ((loyaltyTier?.reservationHoldExtraMinutes ?: 0) > 0) {
                            Text(
                                text = "⏰ +${loyaltyTier!!.reservationHoldExtraMinutes} minutes reservation hold",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoGreen
                                )
                            )
                        }
                        if ((loyaltyTier?.discountPercentage ?: 0f) == 0f) {
                            Text(
                                text = "Complete rides to unlock rewards",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tier Benefits
                Text(
                    text = "Loyalty Tiers",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Bronze Tier
                TierCard(
                    tierName = "Bronze Tier",
                    tierColor = Color(0xFFCD7F32),
                    discount = "5% off",
                    requirement = "• 10 trips in last year\n• No missed reservations\n• All bikes returned",
                    isUnlocked = loyaltyTier?.tier == "BRONZE" || loyaltyTier?.tier == "SILVER" || loyaltyTier?.tier == "GOLD"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Silver Tier
                TierCard(
                    tierName = "Silver Tier",
                    tierColor = Color(0xFFC0C0C0),
                    discount = "10% off + 2 min hold",
                    requirement = "• Bronze eligibility\n• 5 trips/month for 3 months\n• 5+ completed trips last year",
                    isUnlocked = loyaltyTier?.tier == "SILVER" || loyaltyTier?.tier == "GOLD"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gold Tier
                TierCard(
                    tierName = "Gold Tier",
                    tierColor = Color(0xFFFFD700),
                    discount = "15% off + 5 min hold",
                    requirement = "• Silver eligibility\n• 5 trips/week for 12 weeks",
                    isUnlocked = loyaltyTier?.tier == "GOLD"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Section
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Rides Completed",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "${loyaltyTier?.totalRides ?: 0}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EcoGreen
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress bar
                        val progress = ((loyaltyTier?.totalRides ?: 0) / 10f).coerceIn(0f, 1f)
                        Column {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = EcoGreen,
                                trackColor = Color.LightGray.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (loyaltyTier?.tier == "BRONZE") {
                                    "🎉 Bronze tier unlocked!"
                                } else {
                                    "${10 - (loyaltyTier?.totalRides ?: 0)} more rides to Bronze tier"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TierCard(
    tierName: String,
    tierColor: Color,
    discount: String,
    requirement: String,
    isUnlocked: Boolean,
    isComingSoon: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) tierColor.copy(alpha = 0.1f) else PureWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isUnlocked) androidx.compose.foundation.BorderStroke(2.dp, tierColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isUnlocked) tierColor else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = tierName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isComingSoon) Color.Gray else DarkGreen
                        )
                    )
                    Text(
                        text = discount,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUnlocked) tierColor else EcoGreen
                        )
                    )
                    Text(
                        text = requirement,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            if (isUnlocked) {
                Text(
                    text = "UNLOCKED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                )
            }
        }
    }
}
