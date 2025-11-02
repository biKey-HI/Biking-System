package com.example.bikey.ui.operator

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bikey.ui.login.LoginState

import com.example.bikey.ui.network.mapAPI
import com.example.bikey.ui.operator.model.DockingStationResponse
import com.example.bikey.ui.theme.EcoGreen
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorMapDashboardScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val api = mapAPI
    var stations by remember { mutableStateOf<List<DockingStationResponse>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val _events = MutableSharedFlow<MapEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    LaunchedEffect(Unit) {
        try {
            Log.d("MapScreen", "Starting API Call")
            val response = api.map()
            Log.d("MapScreen", "Response Obtained")
            if (response.isSuccessful) {
                Log.d("MapScreen", "Success")
                response.body()?.let { body ->
                    stations = body
                }
                Log.d("MapScreen", "Exiting")
            } else {
                Log.d("MapScreen", "Exception")
                throw Exception("Bad response: ${response.code()}")
            }
        } catch (ex: Exception) {
            val err = "Network error. Please check your connection and try again."
            Log.d("MapScreen", err)
            errorMsg = err
            set { copy(isLoading = false, errorMsg = err) }
            _events.emit(MapEvent.ShowMessage(err))
        }
    }

    Log.d("MapScreen", "Preparing")
    val initialPosition = stations.firstOrNull()?.location
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(initialPosition?.latitude ?: 45.5017, initialPosition?.longitude ?: -73.5673), 12f)
    }
    Log.d("MapScreen", "Done")

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
                    text = "⚠️ Failed to load station data or no stations found.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp).align(Alignment.Center)
                )
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    stations.forEach { station ->
                        Marker(
                            state = MarkerState(position = LatLng(station.location.latitude, station.location.longitude)),
                            title = station.name,
                            snippet = "Bikes: ${station.numOccupiedDocks}/${station.capacity} | Status: ${station.status}",

                        )
                    }
                }
            }
        }
    }
}

var state by mutableStateOf(LoginState())
    private set

private fun set(upd: LoginState.() -> LoginState) { state = state.upd() }

sealed interface MapEvent {
    data class ShowMessage(val message: String) : MapEvent
}
