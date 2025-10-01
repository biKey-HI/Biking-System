package com.example.bikey.ui.login

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Top-level extension property on Context (must be top-level, not inside a class)
val Context.dataStore by preferencesDataStore(name = "auth")

private object Keys {
    val TOKEN: Preferences.Key<String> = stringPreferencesKey("token")
}

class AuthStore(private val context: Context) {

    // Read token as a Flow<String> (empty string if not set)
    val tokenFlow = context
        .dataStore
        .data
        .map { prefs -> prefs[Keys.TOKEN].orEmpty() }

    // Save token
    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs -> prefs[Keys.TOKEN] = token }
    }

    // One-shot read
    suspend fun currentToken(): String = tokenFlow.first()

    // Clear token
    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs.remove(Keys.TOKEN) }
    }
}
