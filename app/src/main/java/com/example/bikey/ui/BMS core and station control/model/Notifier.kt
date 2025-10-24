package com.example.bikey.ui.bmscoreandstationcontrol.model

interface Notifier {
    val observers: MutableList<Sender>
    fun notify(message: Array<Any>): Unit
}