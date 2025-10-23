package org.example.app.observer

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Concrete notifier for trip ending events
 * Monitors bicycle trips and notifies observers when trips are ending
 */
@Component
class TripEndingNotifier @Autowired constructor(
    private val appObserver: AppObserver,
    private val emailObserver: EmailObserver
) : Notifier {
    private val observers = CopyOnWriteArrayList<Observer>()
    
    init {
        // Assign specific observers for trip ending notifications
        observers.add(appObserver)
        observers.add(emailObserver)
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
                observer.update("Bicycle trip is ending")
            } catch (e: Exception) {
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
    
    /**
     * Notify observers about trip ending or completion
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID where the bike is being returned
     * @param tripDurationMinutes The duration of the trip in minutes
     * @param totalCost The total cost of the trip (optional)
     */
    fun notifyTripEnding(bikeId: String, stationId: String, tripDurationMinutes: Long, totalCost: Float? = null) {
        val message = if (totalCost != null) {
            "Bicycle $bikeId successfully returned to station $stationId after ${tripDurationMinutes} minutes. Total cost: $$totalCost"
        } else {
            "Bicycle $bikeId trip ending at station $stationId after ${tripDurationMinutes} minutes"
        }
        notifyObservers(message)
    }
}
