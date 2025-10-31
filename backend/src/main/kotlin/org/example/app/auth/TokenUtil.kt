package org.example.app.auth

import java.util.Base64
import java.util.UUID

object TokenUtil {
    fun issueFakeToken(userId: UUID, email: String): String {
        // should use jjwt or spring-security-oauth2-jose for real tokens
        val payload = "$userId:$email:${System.currentTimeMillis()}"
        return Base64.getEncoder().encodeToString(payload.toByteArray())
    }
}
