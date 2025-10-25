package com.example.bikey.ui.bmscoreandstationcontrol.model

import android.util.Log

class Emailer private constructor(): Sender {
    companion object {val instance: Emailer = Emailer()}
    override fun send(title: String, message: String): Unit {
        Log.i(title, message)
    }
}