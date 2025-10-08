package org.example.app.auth

import org.springframework.stereotype.Service
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper

@Service
class EmailService(private val mailSender: JavaMailSender) {
    fun sendPasswordResetEmail(toEmail: String, resetLink: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setTo(toEmail)
        helper.setSubject("Password Reset Request")
        helper.setText("""
            Click the link below to reset your password:
            $resetLink
            
            This link will expire in 1 hour.
        """.trimIndent())

        mailSender.send(message)
    }
}