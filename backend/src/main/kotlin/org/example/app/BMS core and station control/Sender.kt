package org.example.app.bmscoreandstationcontrol

interface Sender {
    fun send(title: String, message: String, notificationToken: String? = null): Unit
}