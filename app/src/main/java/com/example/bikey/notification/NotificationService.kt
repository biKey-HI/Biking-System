package com.example.bikey.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.sse.*
import java.util.concurrent.TimeUnit

/**
 * Android service for receiving real-time notifications from the backend
 * Uses Server-Sent Events (SSE) to maintain a persistent connection
 */
class NotificationService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var client: OkHttpClient? = null
    private var eventSource: EventSource? = null
    
    companion object {
        private const val TAG = "NotificationService"
        private const val BASE_URL = "http://10.0.2.2:8080" // Android emulator localhost
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationService created")
        
        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // No timeout for SSE
            .build()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra("userId") ?: "default-user"
        startNotificationStream(userId)
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Start the SSE connection for real-time notifications
     */
    private fun startNotificationStream(userId: String) {
        serviceScope.launch {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/notifications/stream/$userId")
                    .build()
                
                val eventSourceListener = object : EventSourceListener() {
                    override fun onOpen(eventSource: EventSource, response: Response) {
                        Log.d(TAG, "SSE connection opened")
                        showNotification("Connected", "Real-time notifications enabled")
                    }
                    
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        Log.d(TAG, "Received notification: $data")
                        handleNotification(data)
                    }
                    
                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        Log.e(TAG, "SSE connection failed", t)
                        // Attempt to reconnect after a delay
                        serviceScope.launch {
                            delay(5000)
                            startNotificationStream(userId)
                        }
                    }
                    
                    override fun onClosed(eventSource: EventSource) {
                        Log.d(TAG, "SSE connection closed")
                    }
                }
                
                eventSource = EventSources.createFactory(client!!).newEventSource(request, eventSourceListener)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting notification stream", e)
            }
        }
    }
    
    /**
     * Handle incoming notifications
     */
    private fun handleNotification(data: String) {
        try {
            // Parse the notification data (simplified JSON parsing)
            val message = extractMessageFromData(data)
            showNotification("Bike System", message)
            
            // You can add more sophisticated parsing here
            // and trigger specific actions based on notification type
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification", e)
        }
    }
    
    /**
     * Extract message from notification data
     */
    private fun extractMessageFromData(data: String): String {
        // Simple extraction - in a real app, use proper JSON parsing
        return if (data.contains("\"message\"")) {
            val start = data.indexOf("\"message\":\"") + 11
            val end = data.indexOf("\"", start)
            data.substring(start, end)
        } else {
            data
        }
    }
    
    /**
     * Show a notification to the user
     */
    private fun showNotification(title: String, message: String) {
        // This would integrate with Android's NotificationManager
        // For now, just log the notification
        Log.i(TAG, "Notification: $title - $message")
        
        // TODO: Implement actual notification display
        // You would use NotificationManagerCompat to show system notifications
    }
    
    override fun onDestroy() {
        super.onDestroy()
        eventSource?.cancel()
        client?.dispatcher?.executorService?.shutdown()
        serviceScope.cancel()
        Log.d(TAG, "NotificationService destroyed")
    }
}
