package org.example.app.bmscoreandstationcontrol.domain

interface Sender {
    fun send(title: String, message: String, notificationToken: String? = null): Unit
}