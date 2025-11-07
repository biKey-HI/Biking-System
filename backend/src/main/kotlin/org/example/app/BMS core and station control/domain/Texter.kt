package org.example.app.bmscoreandstationcontrol.domain

class Texter private constructor(): Sender {
    companion object {val instance: Texter = Texter()}
    override fun send(title: String, message: String, notificationToken: String?): Unit {
        println(title + ": " + message)
    }
}