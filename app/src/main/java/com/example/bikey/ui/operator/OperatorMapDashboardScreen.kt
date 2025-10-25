package com.example.bikey.ui.operator

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bikey.data.BmsConfigurationLoader
import com.example.bikey.data.DockingStationConfig
import com.example.bikey.ui.theme.EcoGreen
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorMapDashboardScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // 1. LOAD CONFIGURATION DATA
    val stations: List<DockingStationConfig> = remember {
        // Load data from the bms_config.json asset file
        BmsConfigurationLoader.loadStationsFromAsset(context)
    }

    // Determine initial map position (Montreal)
    val defaultLocation = LatLng(45.5017, -73.5673)
    val initialPosition = stations.firstOrNull()?.toLatLng() ?: defaultLocation
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 12f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BMS Live Map 🗺️", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back", tint = EcoGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (stations.isEmpty()) {
                Text(
                    text = "⚠️ Failed to load station data or no stations found. Check your 'bms_config.json' file.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp).align(Alignment.Center)
                )
            } else {
                // 2. DISPLAY THE MAP AND MARKERS
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    stations.forEach { station ->
                        Marker(
                            state = MarkerState(position = station.toLatLng()),
                            title = station.name,
                            snippet = "Bikes: ${station.numberOfBikesDocked}/${station.capacity} | Status: ${station.status}",

                        )
                    }
                }
            }
        }
    }
}