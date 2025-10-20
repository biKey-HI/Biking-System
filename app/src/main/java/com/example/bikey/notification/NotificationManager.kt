package com.example.bikey.notification

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Manager class for handling notifications in the Android app
 * Provides methods to start/stop notification services and register for notifications
 */
class NotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationManager"
    }
    
    /**
     * Start the notification service for a user
     * @param userId The user ID
     */
    fun startNotificationService(userId: String) {
        try {
            val intent = Intent(context, NotificationService::class.java).apply {
                putExtra("userId", userId)
            }
            context.startService(intent)
            Log.d(TAG, "Started notification service for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting notification service", e)
        }
    }
    
    /**
     * Stop the notification service
     */
    fun stopNotificationService() {
        try {
            val intent = Intent(context, NotificationService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Stopped notification service")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping notification service", e)
        }
    }
    
    /**
     * Register user email for notifications
     * @param userId The user ID
     * @param email The user's email address
     */
    suspend fun registerEmail(userId: String, email: String): Boolean {
        return try {
            // This would make an API call to register the email
            // For now, just log the registration
            Log.d(TAG, "Registered email $email for user $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering email", e)
            false
        }
    }
    
    /**
     * Register user phone for SMS notifications
     * @param userId The user ID
     * @param phoneNumber The user's phone number
     */
    suspend fun registerPhone(userId: String, phoneNumber: String): Boolean {
        return try {
            // This would make an API call to register the phone
            // For now, just log the registration
            Log.d(TAG, "Registered phone $phoneNumber for user $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering phone", e)
            false
        }
    }
}
