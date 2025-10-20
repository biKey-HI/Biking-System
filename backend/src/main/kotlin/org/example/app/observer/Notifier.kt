package org.example.app.observer

/**
 * Notifier interface for the Observer pattern
 * Defines the contract for subjects that can be observed
 */
interface Notifier {
    /**
     * Attach an observer to this notifier
     * @param observer The observer to attach
     */
    fun attach(observer: Observer)
    
    /**
     * Detach an observer from this notifier
     * @param observer The observer to detach
     */
    fun detach(observer: Observer)
    
    /**
     * Notify all attached observers of a change
     */
    fun notifyObservers()
}
