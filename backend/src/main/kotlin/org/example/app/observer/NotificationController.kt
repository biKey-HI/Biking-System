package org.example.app.observer

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.TimeUnit

/**
 * REST Controller for notification endpoints
 * Provides SSE endpoints for real-time notifications
 */
@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {
    
    /**
     * Establish SSE connection for real-time notifications
     * @param userId The user ID
     * @return SSE emitter for real-time notifications
     */
    @GetMapping(value = ["/stream/{userId}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamNotifications(@PathVariable userId: String): SseEmitter {
        val emitter = SseEmitter(TimeUnit.MINUTES.toMillis(30)) // 30 minute timeout
        
        notificationService.addAppConnection(userId, emitter)
        
        return emitter
    }
    
    /**
     * Register user email for notifications
     * @param userId The user ID
     * @param email The user's email address
     * @return Success response
     */
    @PostMapping("/email/{userId}")
    fun registerEmail(
        @PathVariable userId: String,
        @RequestBody emailRequest: EmailRegistrationRequest
    ): ResponseEntity<Map<String, String>> {
        notificationService.addUserEmail(userId, emailRequest.email)
        return ResponseEntity.ok(mapOf("message" to "Email registered for notifications"))
    }
    
    /**
     * Register user phone for SMS notifications
     * @param userId The user ID
     * @param phoneRequest The phone registration request
     * @return Success response
     */
    @PostMapping("/phone/{userId}")
    fun registerPhone(
        @PathVariable userId: String,
        @RequestBody phoneRequest: PhoneRegistrationRequest
    ): ResponseEntity<Map<String, String>> {
        notificationService.addUserPhone(userId, phoneRequest.phoneNumber)
        return ResponseEntity.ok(mapOf("message" to "Phone registered for SMS notifications"))
    }
    
    /**
     * Get notification statistics
     * @return Notification statistics
     */
    @GetMapping("/stats")
    fun getNotificationStats(): ResponseEntity<Map<String, Any>> {
        val stats = notificationService.getNotificationStats()
        return ResponseEntity.ok(stats)
    }
    
    /**
     * Test notification endpoint (for development)
     * @param message The test message
     * @return Success response
     */
    @PostMapping("/test")
    fun testNotification(@RequestBody testRequest: TestNotificationRequest): ResponseEntity<Map<String, String>> {
        // This would trigger a test notification
        notificationService.notifyOvertime("test-bike-123", 50)
        return ResponseEntity.ok(mapOf("message" to "Test notification sent"))
    }
}

/**
 * Request DTO for email registration
 */
data class EmailRegistrationRequest(
    val email: String
)

/**
 * Request DTO for phone registration
 */
data class PhoneRegistrationRequest(
    val phoneNumber: String
)

/**
 * Request DTO for test notifications
 */
data class TestNotificationRequest(
    val message: String
)
