package com.example.bikey.ui.network

import com.example.bikey.BuildConfig
import com.example.bikey.ui.operator.model.DockingStationResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MapAPI {
    @GET("api/map")
    suspend fun map(): Response<List<DockingStationResponse>>
}
private val contentType = "application/json".toMediaType()

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()
}

val mapRetrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(json.asConverterFactory(contentType))
        .client(httpClient)
        .build()
}

val mapAPI: MapAPI by lazy { mapRetrofit.create(MapAPI::class.java) }

val directionsApi: DirectionsApi by lazy {
    Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/maps/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DirectionsApi::class.java)
}

interface DirectionsApi {
    @GET("directions/json")
    suspend fun getDistance(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String = BuildConfig.MAPS_API_KEY
    ): DirectionsResponse
}
data class DirectionsResponse(val routes: List<Route>)
data class Route(val legs: List<Leg>)
data class Leg(val distance: DistanceValue)
data class DistanceValue(val value: Int)

