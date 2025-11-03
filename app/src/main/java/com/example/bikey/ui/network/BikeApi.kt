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
data class TakeBikeResponse(val bikeId: String, val startedAtEpochMs: Long)

interface BikeAPI {
    @POST("api/take-bike")
    suspend fun takeBike(@Body body: TakeBikeRequest): Response<TakeBikeResponse>
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
