package org.example.app.bmscoreandstationcontrol

class Emailer private constructor(): Sender {
    companion object {val instance: Emailer = Emailer()}
    override fun send(title: String, message: String, notificationToken: String?): Unit {
        println(title + ": " + message)
    }
}