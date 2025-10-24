package com.example.bikey.ui.reservation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikey.ui.bmscoreandstationcontrol.model.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

// --- FAKE OBJECTS FOR DEMO ---
// In a real app, these would be injected using Hilt/Dagger
object UserSessionManager {
    // FAKE: Assume the user ID is 1L and we have a token
    val currentUserId: Long = 1L
    val authToken: String = "fake-jwt-token"
}

// FAKE: This would be your Retrofit service
object ReservationApi {
    // Simulates the backend call
    suspend fun createReservation(userId: Long, bikeId: UUID, token: String): Boolean {
        kotlinx.coroutines.delay(1500) // Simulate network latency
        if (bikeId.toString().endsWith("0")) {
            throw RuntimeException("Bike is no longer available.")
        }
        return true
    }
}
// --- END OF FAKE OBJECTS ---

class ReservationViewModel : ViewModel() {

    var state by mutableStateOf(ReservationState())
        private set

    private val _events = Channel<ReservationEvent>()
    val events = _events.receiveAsFlow()

    fun loadDefaultStationData() {
        // Create a hardcoded DockingStation for the demo
        val demoStation = createDemoStation()

        state = state.copy(
            dockingStation = demoStation,
            // Filter for bikes that are actually available in the docks
            availableBikes = demoStation.docks
                .mapNotNull { it.bike }
                .filter { it.status == BikeState.AVAILABLE }
        )
    }

    fun reserveBike(bikeId: UUID) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            try {
                val userId = UserSessionManager.currentUserId
                val authToken = UserSessionManager.authToken

                val success = ReservationApi.createReservation(userId, bikeId, authToken)

                if (success) {
                    state = state.copy(isLoading = false, reservationSuccessBikeId = bikeId)
                    _events.send(ReservationEvent.Success(bikeId))
                } else {
                    throw RuntimeException("An unknown error occurred.")
                }

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to reserve bike."
                state = state.copy(isLoading = false, error = errorMessage)
                _events.send(ReservationEvent.Failure(errorMessage))
            }
        }
    }

    /**
     * Creates a sample DockingStation with a mix of available and unavailable bikes.
     */
    private fun createDemoStation(): DockingStation {
        val bike1 = Bike(id = UUID.fromString("11111111-1111-1111-1111-11111111111a"), status = BikeState.AVAILABLE)
        val bike2 = EBike(id = UUID.fromString("22222222-2222-2222-2222-22222222222b"), status = BikeState.AVAILABLE)
        val bike3 = Bike(id = UUID.fromString("33333333-3333-3333-3333-33333333333c"), status = BikeState.MAINTENANCE) // Not available
        val bike4 = Bike(id = UUID.fromString("44444444-4444-4444-4444-444444444440"), status = BikeState.AVAILABLE) // Will fail API call

        return DockingStation(
            name = "Demo Station | Mile End",
            address = Address(number = 5420, street = "St Laurent Blvd", postalCode = "H2T 1S1"),
            location = LatLng(45.5250, -73.5950),
            capacity = 10,
            docks = mutableListOf(
                Dock(bike = bike1, status = DockState.OCCUPIED),
                Dock(bike = bike2, status = DockState.OCCUPIED),
                Dock(bike = bike3, status = DockState.OCCUPIED),
                Dock(bike = bike4, status = DockState.OCCUPIED),
                Dock(status = DockState.EMPTY),
                Dock(status = DockState.EMPTY),
                Dock(status = DockState.EMPTY),
                Dock(status = DockState.OUT_OF_SERVICE),
                Dock(status = DockState.EMPTY),
                Dock(status = DockState.EMPTY)
            )
        )
    }
}