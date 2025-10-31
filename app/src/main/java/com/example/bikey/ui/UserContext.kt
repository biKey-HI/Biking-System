package com.example.bikey.ui
import java.util.UUID

class User(val id: UUID = UUID.randomUUID(), val email: String, val isOperator: Boolean = false)

class UserContext {
    companion object {
        var user: User? = null
        val id: UUID? = user?.id
        val isOperator: Boolean? = user?.isOperator
        val email: String? = user?.email
        var notificationToken: String? = null
    }
}