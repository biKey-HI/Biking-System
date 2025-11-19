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
    val discountCents: Int = 0,
    val loyaltyTier: String? = null,
    val flexDollarCents: Int = 0,
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

@Serializable
data class ReserveBikeRequest(
    val stationId: String,
    val bikeId: String,
    val userId: String
)

@Serializable
data class ReserveBikeResponse(
    val stationId: String,
    val bikeId: String,
    val reservedUntilEpochMs: Long
)

@Serializable
data class MoveBikeRequest(
    val fromStationId: String,
    val userId: String,
    val bikeId: String,
    val toDockId: String? = null,
    val toStationId: String
)

@Serializable
data class ToggleStationOutOfServiceRequest(
    val dockingStationId: String,
    val userId: String
)

@Serializable
data class ToggleBikeMaintenanceRequest(
    val dockingStationId: String,
    val userId: String,
    val bikeId: String
)

// Loyalty Program DTOs
@Serializable
data class LoyaltyTierResponse(
    val userId: String,
    val tier: String,
    val tierDisplayName: String,
    val discountPercentage: Float,
    val totalRides: Int,
    val reservationHoldExtraMinutes: Int
)

@Serializable
data class BronzeEligibilityResponse(
    val userId: String,
    val isEligible: Boolean,
    val message: String
)

@Serializable
data class AllTierEligibilityResponse(
    val userId: String,
    val bronzeEligible: Boolean,
    val silverEligible: Boolean,
    val goldEligible: Boolean
)

@Serializable
data class TierUpdateResponse(
    val userId: String,
    val oldTier: String,
    val newTier: String,
    val tierChanged: Boolean,
    val upgraded: Boolean,
    val downgraded: Boolean,
    val currentTierInfo: LoyaltyTierResponse
)

@Serializable
data class LoyaltyProgressDTO(
    val currentTier: String,
    val currentTierDisplayName: String,
    val currentDiscount: Int,
    val nextTier: String?,
    val nextTierDisplayName: String?,
    val nextTierDiscount: Int?,
    val requirementsForNext: Map<String, String>,
    val currentProgress: Map<String, String>,
    val totalCompletedTrips: Int
)

interface BikeAPI {
    @POST("api/take-bike")
    suspend fun takeBike(@Body body: TakeBikeRequest): Response<TakeBikeResponse>

    @POST("api/return")
    suspend fun returnBike(@Body body: ReturnBikeRequest): Response<ReturnAndSummaryResponse>

    @retrofit2.http.GET("api/ride-history/{userId}")
    suspend fun getRideHistory(@retrofit2.http.Path("userId") userId: String): Response<List<RideHistoryItemDTO>>
    @POST("api/reserve-bike")
    suspend fun reserveBike(@Body body: ReserveBikeRequest): Response<ReserveBikeResponse>

    @POST("api/move-bike")
    suspend fun moveBike(@Body body: MoveBikeRequest): Response<Boolean?>

    @POST("api/out-of-service-station")
    suspend fun toggleStationOutOfService(@Body body: ToggleStationOutOfServiceRequest): Response<Unit?>

    @POST("api/maintenance-bike")
    suspend fun toggleBikeMaintenance(@Body body: ToggleBikeMaintenanceRequest): Response<Unit?>

    @retrofit2.http.GET("api/loyalty/tier/{userId}")
    suspend fun getLoyaltyTier(@retrofit2.http.Path("userId") userId: String): Response<LoyaltyTierResponse>

    @retrofit2.http.GET("api/loyalty/check-bronze-eligibility/{userId}")
    suspend fun checkBronzeEligibility(@retrofit2.http.Path("userId") userId: String): Response<BronzeEligibilityResponse>

    @POST("api/loyalty/update-tier/{userId}")
    suspend fun updateLoyaltyTier(@retrofit2.http.Path("userId") userId: String): Response<LoyaltyTierResponse>

    @retrofit2.http.GET("api/loyalty/progress/{userId}")
    suspend fun getLoyaltyProgress(@retrofit2.http.Path("userId") userId: String): Response<LoyaltyProgressDTO>
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