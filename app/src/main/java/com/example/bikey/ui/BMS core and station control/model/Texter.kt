package com.example.bikey.ui.bmscoreandstationcontrol.model
import android.util.Log

class Texter private constructor(): Sender {
    companion object {val instance: Texter = Texter()}
    override fun send(title: String, message: String): Unit {
        Log.i(title, message)
    }
}