package com.example.bikey.ui.bmscoreandstationcontrol.model

interface Sender {
    fun send(title: String, message: String): Unit
}