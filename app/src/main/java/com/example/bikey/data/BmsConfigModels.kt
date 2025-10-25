package com.example.bikey.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

// --- Data Transfer Objects (DTOs) for JSON Deserialization ---

@Serializable
data class BikeConfig(
    val id: String,
    val type: String,
    val status: String
)

@Serializable
data class DockingStationConfig(
    val id: String,
    val name: String,
    val address: String, 
    val lat: Double,
    val long: Double,
    val capacity: Int,
    val status: String,
    val dockedBikes: List<BikeConfig> = emptyList()
) {
    // Utility for map marker placement
    fun toLatLng(): LatLng = LatLng(lat, long)

    val numberOfBikesDocked: Int
        get() = dockedBikes.size
}

@Serializable
data class BmsSystemConfig(
    val stations: List<DockingStationConfig>
)

// --- Loader Utility ---

object BmsConfigurationLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Loads the configuration from the "bms_config.json" file in the app's assets.
     */
    fun loadStationsFromAsset(context: Context, fileName: String = "bms_config.json"): List<DockingStationConfig> {
        val jsonString: String
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            println("Error reading asset file $fileName: ${ioException.message}")
            return emptyList()
        }

        return try {
            val config = json.decodeFromString<BmsSystemConfig>(jsonString)
            config.stations
        } catch (e: SerializationException) {
            println("Error deserializing config JSON: ${e.message}")
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            println("Unexpected error during config loading: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}