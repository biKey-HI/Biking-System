package com.example.bikey.ui.network

import com.example.bikey.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.UUID

interface PricingPlanApi {
    @GET("api/plan")
    suspend fun changePricingPlan(@Query("userId") userId: UUID, @Query("pricingPlan") pricingPlan: String): Response<Boolean>
}

// A small Retrofit provider (or move this to a DI module)
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

val pricingPlanRetrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(json.asConverterFactory(contentType))
        .client(httpClient)
        .build()
}

val pricingPlanApi: PricingPlanApi by lazy { pricingPlanRetrofit.create(PricingPlanApi::class.java) }

