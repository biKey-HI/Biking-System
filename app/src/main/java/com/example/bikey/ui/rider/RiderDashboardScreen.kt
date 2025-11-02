package com.example.bikey.ui.rider

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboardScreen(
    riderEmail: String,
    onLogout: () -> Unit
) {
    val username = riderEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
    var stations by remember { mutableStateOf<List<DockingStationResponse>>(emptyList()) }
    var selectedStation by remember { mutableStateOf<DockingStationResponse?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var panelExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load stations
    LaunchedEffect(Unit) {
        try {
            val response = mapAPI.map()
            if (response.isSuccessful) {
                stations = response.body() ?: emptyList()
            }
        } catch (ex: Exception) {
            // Handle error silently for now
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.5017, -73.5673), 13f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map Layer
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            stations.forEach { station ->
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
                onClick = { /* TODO: Search functionality */ },
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
                    text = "Hi, $username! 👋",
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
                        text = "Hi, $username! 👋",
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
                icon = Icons.Default.DirectionsBike,
                label = "Available Bikes",
                value = station.numOccupiedDocks.toString(),
                color = EcoGreen
            )
            StationStat(
                icon = Icons.Default.Place,
                label = "Free Docks",
                value = station.numFreeDocks.toString(),
                color = MintDark
            )
            StationStat(
                icon = Icons.Default.Dashboard,
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
            onClick = { /* TODO: Reserve bike */ },
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
    ModalDrawerSheet(
        drawerContainerColor = PureWhite,
        modifier = Modifier.width(300.dp)
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
            icon = Icons.Default.CreditCard,
            text = "Payment Methods",
            onClick = { /* TODO */ }
        )

        MenuItemButton(
            icon = Icons.Default.History,
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
            icon = Icons.Default.Help,
            text = "Help & Support",
            onClick = { /* TODO */ }
        )

        Spacer(modifier = Modifier.weight(1f))

        Divider()

        MenuItemButton(
            icon = Icons.Default.Logout,
            text = "Logout",
            onClick = onLogout,
            textColor = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dismiss on outside click
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    )
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
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}
