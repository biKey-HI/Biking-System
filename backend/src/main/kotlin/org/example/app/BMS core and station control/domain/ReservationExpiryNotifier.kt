package org.example.app.bmscoreandstationcontrol.domain

class ReservationExpiryNotifier private constructor(): Notifier {
    companion object {val instance: ReservationExpiryNotifier = ReservationExpiryNotifier()}
    override val observers: MutableList<Sender> = mutableListOf(Notificator.instance)

    override fun notify(message: Array<Any>, notificationToken: String?): Unit {
        require(message.count() == 1 && message[0] is Int) {"Incorrect array input."}
        val expiry: String
        if((message[0] as Int) < 0) expiry = "Your bike reservation is about to expire in " + (message[0] as Int)*-1 + " min."
        else expiry = "Your bike reservation has expired."
        observers.forEach {it.send("Reservation Expiry", expiry, notificationToken)}
    }
}