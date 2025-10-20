package org.example.app.observer

import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Concrete notifier for trip ending events
 * Monitors bicycle trips and notifies observers when trips are ending
 */
@Component
class TripEndingNotifier : Notifier {
    private val observers = CopyOnWriteArrayList<Observer>()
    
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
    
    /**
     * Notify observers about trip ending
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID where the bike is being returned
     * @param tripDurationMinutes The duration of the trip in minutes
     */
    fun notifyTripEnding(bikeId: String, stationId: String, tripDurationMinutes: Long) {
        val message = "Bicycle $bikeId trip ending at station $stationId after ${tripDurationMinutes} minutes"
        observers.forEach { observer ->
            try {
                observer.update(message)
            } catch (e: Exception) {
                println("Error notifying observer about trip ending: ${e.message}")
            }
        }
    }
    
    /**
     * Notify observers about successful trip completion
     * @param bikeId The bicycle ID
     * @param stationId The docking station ID
     * @param totalCost The total cost of the trip
     */
    fun notifyTripCompleted(bikeId: String, stationId: String, totalCost: Float) {
        val message = "Bicycle $bikeId successfully returned to station $stationId. Total cost: $$totalCost"
        observers.forEach { observer ->
            try {
                observer.update(message)
            } catch (e: Exception) {
                println("Error notifying observer about trip completion: ${e.message}")
            }
        }
    }
}
