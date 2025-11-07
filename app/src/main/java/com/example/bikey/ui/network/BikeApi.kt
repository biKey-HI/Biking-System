package com.example.bikey.ui.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

@Serializable
data class TakeBikeRequest(val stationId: String, val userEmail: String)

@Serializable
data class TakeBikeResponse(
    val bikeId: String,
    val tripId: String,
    val startedAtEpochMs: Long
)

@Serializable
data class ReturnBikeRequest(
    val tripId: String,
    val destStationId: String,
    val dockId: String? = null
)

@Serializable
data class CostBreakdownDTO(
    val baseCents: Int,
    val perMinuteCents: Int,
    val minutes: Int,
    val eBikeSurchargeCents: Int? = null,
    val overtimeCents: Int? = null,
    val totalCents: Int
)

@Serializable
data class TripSummaryDTO(
    val tripId: String,
    val riderId: String,
    val bikeId: String,
    val startStationName: String,
    val endStationName: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val isEBike: Boolean,
    val cost: CostBreakdownDTO
)

@Serializable
data class ReturnAndSummaryResponse(
    val summary: TripSummaryDTO,
    val paymentStrategy: String,
    val requiresImmediatePayment: Boolean,
    val hasSavedCard: Boolean,
    val savedCardLast4: String? = null,
    val provider: String? = null
)

// for ride history billing info
@Serializable
data class RideHistoryItemDTO(
    val summary: TripSummaryDTO,
    val paymentStrategy: String,
    val hasSavedCard: Boolean,
    val cardHolderName: String? = null,
    val savedCardLast4: String? = null,
    val provider: String? = null
)

interface BikeAPI {
    @POST("api/take-bike")
    suspend fun takeBike(@Body body: TakeBikeRequest): Response<TakeBikeResponse>

    @POST("api/return")
    suspend fun returnBike(@Body body: ReturnBikeRequest): Response<ReturnAndSummaryResponse>

    @retrofit2.http.GET("api/ride-history/{userId}")
    suspend fun getRideHistory(@retrofit2.http.Path("userId") userId: String): Response<List<RideHistoryItemDTO>>
}

private val bikeClient = OkHttpClient.Builder()
    .callTimeout(8, TimeUnit.SECONDS)
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .build()

private val json = Json { ignoreUnknownKeys = true }

private val bikeRetrofit = Retrofit.Builder()
    .baseUrl("http://10.0.2.2:8080/")
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .client(bikeClient)
    .build()

val bikeAPI: BikeAPI by lazy { bikeRetrofit.create(BikeAPI::class.java) }