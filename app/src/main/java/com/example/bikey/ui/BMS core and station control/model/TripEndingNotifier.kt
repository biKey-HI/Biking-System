package com.example.bikey.ui.bmscoreandstationcontrol.model

import com.example.bikey.ui.bmscoreandstationcontrol.model.Notifier

class TripEndingNotifier private constructor(): Notifier {
    companion object {val instance: TripEndingNotifier = TripEndingNotifier()}
    override val observers: MutableList<Sender> = mutableListOf(Notificator.instance, Emailer.instance)

    override fun notify(message: Array<Any>): Unit {
        require(message.count() == 1 && message[0] is String) {"Incorrect array input."}
        observers.forEach {it.send("Ride Ended","Your bike ride has ended at station " + message[0] as String + ".")}
    }
}