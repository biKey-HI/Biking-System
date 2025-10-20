package org.example.app.observer

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete observer for mobile app notifications
 * Handles real-time notifications to mobile app users via Server-Sent Events
 */
@Component
class AppObserver : Observer {
    private val userName: String = "System"
    
    // Store active SSE connections for real-time notifications
    private val activeConnections = ConcurrentHashMap<String, SseEmitter>()
    
    override fun update(message: String) {
        println("AppObserver [$userName]: $message")
        
        // Send notification to all active app connections
        activeConnections.forEach { (userId, emitter) ->
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(mapOf(
                        "message" to message,
                        "timestamp" to System.currentTimeMillis(),
                        "type" to "bike_notification"
                    ))
                )
            } catch (e: Exception) {
                println("Error sending SSE notification to user $userId: ${e.message}")
                // Remove failed connection
                activeConnections.remove(userId)
            }
        }
    }
    
    /**
     * Add a new SSE connection for a user
     * @param userId The user ID
     * @param emitter The SSE emitter
     */
    fun addConnection(userId: String, emitter: SseEmitter) {
        activeConnections[userId] = emitter
        
        // Handle connection completion/error
        emitter.onCompletion { activeConnections.remove(userId) }
        emitter.onError { activeConnections.remove(userId) }
        emitter.onTimeout { activeConnections.remove(userId) }
    }
    
    /**
     * Remove a user's SSE connection
     * @param userId The user ID
     */
    fun removeConnection(userId: String) {
        activeConnections.remove(userId)
    }
    
    /**
     * Get the number of active connections
     * @return Number of active connections
     */
    fun getActiveConnectionCount(): Int = activeConnections.size
}
