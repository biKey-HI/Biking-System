package org.example.app.observer

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Concrete notifier for overtime events
 * Monitors bicycles for overtime usage and notifies observers
 */
@Component
class OvertimeNotifier @Autowired constructor(
    private val appObserver: AppObserver,
    private val emailObserver: EmailObserver,
    private val messageTextObserver: MessageTextObserver
) : Notifier {
    private val observers = CopyOnWriteArrayList<Observer>()
    private val LIMIT = 45 // 45 minutes limit in minutes
    
    init {
        // Assign specific observers for overtime notifications
        observers.add(appObserver)
        observers.add(emailObserver)
        observers.add(messageTextObserver)
    }
    
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
    
    override fun notifyObservers(message: String) {
        observers.forEach { observer ->
            try {
                observer.update(message)
            } catch (e: Exception) {
                println("Error notifying observer: ${e.message}")
            }
        }
    }
    
    // Note: checkOvertime logic has been moved to Bicycle class
    // This notifier only handles notifications, not the checking logic
    
    /**
     * Notify observers about overtime
     * @param bikeId The bicycle ID
     * @param durationMinutes The duration in minutes
     */
    fun notifyOvertime(bikeId: String, durationMinutes: Long) {
        val message = "Bicycle $bikeId has been in use for ${durationMinutes} minutes, exceeding the ${LIMIT}-minute limit"
        notifyObservers(message)
    }
}
