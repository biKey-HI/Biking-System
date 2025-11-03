package org.example.app.pricingandpayment.api

import org.example.app.billing.BillingService
import java.util.UUID

interface TripFacade {
    /** should call DockingStation.returnBike(...) then return a TripDomain for billing */
    fun completeTripAndFetchDomain(tripId: UUID, destStationId: UUID, dockId: String?): BillingService.TripDomain
    /** read-only accessor used for idempotent charge resolution */
    fun getTripDomain(tripId: UUID): BillingService.TripDomain
}
