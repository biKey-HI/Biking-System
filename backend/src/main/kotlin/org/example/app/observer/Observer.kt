package org.example.app.observer

/**
 * Observer interface for the Observer pattern
 * Defines the contract for objects that want to be notified of changes
 */
interface Observer {
    /**
     * Called by the subject to notify the observer of a change
     * @param message The notification message
     */
    fun update(message: String)
}
