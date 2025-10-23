package org.example.app.observer

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Simplified service for real-time notification management
 * Only handles SSE connections and user registration for notifications
 * Notifiers are now called directly from DockingStation and Bicycle classes
 */
@Service
class NotificationService @Autowired constructor(
    private val appObserver: AppObserver,
    private val emailObserver: EmailObserver,
    private val messageTextObserver: MessageTextObserver
) {
    
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    
    init {
        // Start background monitoring for real-time updates only
        startBackgroundMonitoring()
    }
    
    /**
     * Start background monitoring tasks for real-time updates
     */
    private fun startBackgroundMonitoring() {
        // Monitor for real-time updates every minute
        scheduler.scheduleAtFixedRate({
            // This could be used for system health checks or other real-time monitoring
            println("Notification service monitoring...")
        }, 0, 1, TimeUnit.MINUTES)
    }
    
    /**
     * Add SSE connection for real-time app notifications
     * @param userId The user ID
     * @param emitter The SSE emitter
     */
    fun addAppConnection(userId: String, emitter: SseEmitter) {
        appObserver.addConnection(userId, emitter)
    }
    
    /**
     * Remove SSE connection
     * @param userId The user ID
     */
    fun removeAppConnection(userId: String) {
        appObserver.removeConnection(userId)
    }
    
    /**
     * Add user email for notifications
     * @param userId The user ID
     * @param email The user's email
     */
    fun addUserEmail(userId: String, email: String) {
        emailObserver.addUserEmail(userId, email)
    }
    
    /**
     * Add user phone for SMS notifications
     * @param userId The user ID
     * @param phoneNumber The user's phone number
     */
    fun addUserPhone(userId: String, phoneNumber: String) {
        messageTextObserver.addUserPhone(userId, phoneNumber)
    }
    
    /**
     * Get notification statistics
     * @return Map of notification statistics
     */
    fun getNotificationStats(): Map<String, Any> {
        return mapOf(
            "activeAppConnections" to appObserver.getActiveConnectionCount(),
            "registeredPhones" to messageTextObserver.getRegisteredPhoneCount()
        )
    }
}
