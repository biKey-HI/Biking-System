package org.example.app.bmscoreandstationcontrol

interface Notifier {
    val observers: MutableList<Sender>
    fun notify(message: Array<Any>, notificationToken: String? = null): Unit
}