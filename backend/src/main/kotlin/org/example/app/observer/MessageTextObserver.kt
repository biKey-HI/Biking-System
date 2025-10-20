package org.example.app.observer

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete observer for SMS/text message notifications
 * Sends SMS notifications to users about bike system events
 */
@Component
class MessageTextObserver : Observer {
    private val phoneNumber: String = "+1-555-BIKE-SYS"
    
    // Store user phone numbers for SMS notifications
    private val userPhoneNumbers = ConcurrentHashMap<String, String>()
    
    override fun update(message: String) {
        println("MessageTextObserver [$phoneNumber]: $message")
        
        // Send SMS to all registered users
        userPhoneNumbers.forEach { (userId, userPhone) ->
            try {
                sendSMS(userPhone, message)
            } catch (e: Exception) {
                println("Error sending SMS to $userPhone: ${e.message}")
            }
        }
    }
    
    /**
     * Send SMS notification (simulated - in real implementation, integrate with SMS service)
     * @param phoneNumber Recipient phone number
     * @param message SMS message
     */
    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            // In a real implementation, this would integrate with an SMS service like Twilio
            println("SMS sent to $phoneNumber: $message")
            
            // Simulate SMS sending delay
            Thread.sleep(100)
            
        } catch (e: Exception) {
            println("Failed to send SMS to $phoneNumber: ${e.message}")
        }
    }
    
    /**
     * Add a user's phone number for SMS notifications
     * @param userId The user ID
     * @param phoneNumber The user's phone number
     */
    fun addUserPhone(userId: String, phoneNumber: String) {
        userPhoneNumbers[userId] = phoneNumber
    }
    
    /**
     * Remove a user's phone number from SMS notifications
     * @param userId The user ID
     */
    fun removeUserPhone(userId: String) {
        userPhoneNumbers.remove(userId)
    }
    
    /**
     * Send a specific SMS notification
     * @param phoneNumber The recipient phone number
     * @param message The SMS message
     */
    fun sendSpecificSMS(phoneNumber: String, message: String) {
        sendSMS(phoneNumber, message)
    }
    
    /**
     * Get the number of registered phone numbers
     * @return Number of registered phone numbers
     */
    fun getRegisteredPhoneCount(): Int = userPhoneNumbers.size
}
