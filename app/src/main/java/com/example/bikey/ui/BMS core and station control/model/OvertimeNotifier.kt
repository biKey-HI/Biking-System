package com.example.bikey.ui.bmscoreandstationcontrol.model

class OvertimeNotifier private constructor(): Notifier {
    companion object {val instance: OvertimeNotifier = OvertimeNotifier()}

    override val observers: MutableList<Sender> = mutableListOf(Notificator.instance, Texter.instance, Emailer.instance)

    override fun notify(message: Array<Any>): Unit {
        require(message.count() == 2 && message[0] is Int && message[1] is Float) {"Incorrect array input."}
        val overtime: String
        if(message[0] as Int <= 0) {overtime = "Your bike trip will be overtime in " + (message[0] as Int)*-1 + " min."}
        else overtime = "Your bike trip is overtime by " + message[0] as Int + "min."
        val overcharge: String
        if(message[0] as Int <= 0 || message[1] as Float <= 0f) {overcharge = "You will be overcharged."}
        else overcharge = "You have already been overcharged by " + message[1] as Float + "."
        observers.forEach {it.send("IMPORTANT: Overtime", overtime + " " + overcharge + " Please return your bike to a docking station as soon as possible.")}
    }
}