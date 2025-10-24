package com.example.bikey.ui.reservation

sealed class ReservationEvent {
    data class Success(val bikeId: UUID) : ReservationEvent()
    data class Failure(val message: String) : ReservationEvent()
}