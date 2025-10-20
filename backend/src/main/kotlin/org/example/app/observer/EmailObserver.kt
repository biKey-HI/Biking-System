package org.example.app.observer

import org.springframework.stereotype.Component
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete observer for email notifications
 * Sends email notifications to users about bike system events
 */
@Component
class EmailObserver : Observer {
    private val email: String = "system@bikey.com"
    
    @Autowired
    private lateinit var mailSender: JavaMailSender
    
    // Store user email addresses for notifications
    private val userEmails = ConcurrentHashMap<String, String>()
    
    override fun update(message: String) {
        println("EmailObserver [$email]: $message")
        
        // Send email to all registered users
        userEmails.forEach { (userId, userEmail) ->
            try {
                sendEmail(userEmail, "Bike System Notification", message)
            } catch (e: Exception) {
                println("Error sending email to $userEmail: ${e.message}")
            }
        }
    }
    
    /**
     * Send email notification
     * @param toEmail Recipient email
     * @param subject Email subject
     * @param body Email body
     */
    private fun sendEmail(toEmail: String, subject: String, body: String) {
        try {
            val message = SimpleMailMessage()
            message.setFrom(email)
            message.setTo(toEmail)
            message.setSubject(subject)
            message.setText(body)
            
            mailSender.send(message)
            println("Email sent successfully to $toEmail")
        } catch (e: Exception) {
            println("Failed to send email to $toEmail: ${e.message}")
        }
    }
    
    /**
     * Add a user's email for notifications
     * @param userId The user ID
     * @param userEmail The user's email address
     */
    fun addUserEmail(userId: String, userEmail: String) {
        userEmails[userId] = userEmail
    }
    
    /**
     * Remove a user's email from notifications
     * @param userId The user ID
     */
    fun removeUserEmail(userId: String) {
        userEmails.remove(userId)
    }
    
    /**
     * Send a specific email notification
     * @param userEmail The recipient email
     * @param subject The email subject
     * @param message The email message
     */
    fun sendSpecificEmail(userEmail: String, subject: String, message: String) {
        sendEmail(userEmail, subject, message)
    }
}
