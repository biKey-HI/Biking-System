package org.example.app.observer

import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Concrete notifier for overtime events
 * Monitors bicycles for overtime usage and notifies observers
 */
@Component
class OvertimeNotifier : Notifier {
    private val observers = CopyOnWriteArrayList<Observer>()
    private val LIMIT = 45 // 45 minutes limit in minutes
    
    override fun attach(observer: Observer) {
        observers.add(observer)
    }
    
    override fun detach(observer: Observer) {
        observers.remove(observer)
    }
    
    override fun notifyObservers() {
        observers.forEach { observer ->
            try {
                observer.update("Bicycle overtime limit exceeded")
            } catch (e: Exception) {
                // Log error but don't break the notification chain
                println("Error notifying observer: ${e.message}")
            }
        }
    }
    
    /**
     * Check if a bicycle has exceeded the overtime limit
     * @param durationMinutes The duration in minutes
     * @return true if overtime, false otherwise
     */
    fun checkOvertime(durationMinutes: Long): Boolean {
        return durationMinutes > LIMIT
    }
    
    /**
     * Notify observers about overtime
     * @param bikeId The bicycle ID
     * @param durationMinutes The duration in minutes
     */
    fun notifyOvertime(bikeId: String, durationMinutes: Long) {
        val message = "Bicycle $bikeId has been in use for ${durationMinutes} minutes, exceeding the ${LIMIT}-minute limit"
        observers.forEach { observer ->
            try {
                observer.update(message)
            } catch (e: Exception) {
                println("Error notifying observer about overtime: ${e.message}")
            }
        }
    }
}
