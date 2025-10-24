package com.example.bikey.ui.bmscoreandstationcontrol.model

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bikey.R

class Notificator private constructor(): Sender {
    companion object {
        val instance: Notificator = Notificator()
        var context: Context? = null
    }
    override fun send(title: String, message: String): Unit {
        if (context != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context!!.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {return}
            val notification = NotificationCompat.Builder(context!!, "app_notification")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(context!!).notify(1, notification)
        }
    }
}