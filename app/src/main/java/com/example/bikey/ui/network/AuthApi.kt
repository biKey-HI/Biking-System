package com.example.bikey.ui.network

import com.example.bikey.BuildConfig
import com.example.bikey.ui.registration.model.RegisterRequest
import com.example.bikey.ui.registration.model.RegisterResponse
import com.example.bikey.ui.login.model.LoginRequest
import com.example.bikey.ui.login.model.LoginResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
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

val authRetrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(json.asConverterFactory(contentType))
        .client(httpClient)
        .build()
}

val authApi: AuthApi by lazy { authRetrofit.create(AuthApi::class.java) }

