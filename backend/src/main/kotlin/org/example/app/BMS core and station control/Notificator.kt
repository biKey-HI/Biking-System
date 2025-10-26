package org.example.app.bmscoreandstationcontrol
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Service

@Service
class Notificator private constructor(): Sender {
    companion object {
        val instance: Notificator = Notificator()
    }
    override fun send(title: String, message: String, notificationToken: String?): Unit {
        if (notificationToken.isNullOrBlank()) return

        val notification = Message.builder()
            .setToken(notificationToken)
            .setNotification(Notification.builder().setTitle(title).setBody(message).build())
            .build()

        FirebaseMessaging.getInstance().send(notification)
    }
}