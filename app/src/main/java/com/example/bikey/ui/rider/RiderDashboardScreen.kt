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
import androidx.compose.material.icons.automirrored.filled.List
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
import com.example.bikey.ui.operator.model.DockingStationResponse
import com.example.bikey.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.layout.systemBarsPadding
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboardScreen(
    riderEmail: String,
    onLogout: () -> Unit,
    onReserveBike: () -> Unit
) {
    val username = riderEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
    var stations by remember { mutableStateOf<List<DockingStationResponse>>(emptyList()) }
    var selectedStation by remember { mutableStateOf<DockingStationResponse?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var panelExpanded by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(BikeFilter.ALL) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }


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

    Box(modifier = Modifier.fillMaxSize()
        .systemBarsPadding()) {
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
                    state = MarkerState(position = LatLng(station.location.latitude, station.location.longitude)),
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
                BikeFilter.values().forEach { filter ->
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
                    }
                )
            }
        }
    }
}

@Composable
fun SlideUpPanel(
    username: String,
    selectedStation: DockingStationResponse?,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val panelHeight by animateFloatAsState(
        targetValue = if (isExpanded) 0.6f else 0.15f,
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
                    StationDetails(selectedStation)
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
fun StationDetails(station: DockingStationResponse) {
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

        // Action Button
        Button(
            onClick = { onReserveBike() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
            shape = RoundedCornerShape(16.dp),
            enabled = station.numOccupiedDocks > 0
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = PureWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (station.numOccupiedDocks > 0) "Reserve a Bike" else "No Bikes Available",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
            )
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
                    Text(
                        text = "Rider Account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Menu Items
            MenuItemButton(
                icon = Icons.Default.Person,
                text = "Edit Profile",
                onClick = { /* TODO */ }
            )

            MenuItemButton(
                icon = Icons.Default.AccountBox,
                text = "Payment Methods",
                onClick = { /* TODO */ }
            )

            MenuItemButton(
                icon = Icons.Default.DateRange,
                text = "Ride History",
                onClick = { /* TODO */ }
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

            Spacer(modifier = Modifier.weight(1f))

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

@Composable
fun MenuItemButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: Color = DarkGreen
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
