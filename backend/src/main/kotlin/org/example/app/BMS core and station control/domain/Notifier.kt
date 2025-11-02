package org.example.app.bmscoreandstationcontrol.domain

interface Notifier {
    val observers: MutableList<Sender>
    fun notify(message: Array<Any>, notificationToken: String? = null): Unit
}