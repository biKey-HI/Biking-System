package org.example.app.pricingandpayment.api

import org.example.app.billing.BillingService
import org.example.app.billing.ReturnAndSummaryResponse
import org.example.app.pricingandpayment.PaymentService
import org.example.app.user.PaymentStrategyType
import org.example.app.user.UserRepository
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/return")
class ReturnController(
    private val users: UserRepository,
    private val billing: BillingService,
    private val payments: PaymentService,
    private val tripFacade: TripFacade
) {
    data class ReturnRequest(val tripId: UUID, val destStationId: UUID, val dockId: String?)
    data class ChargeRequest(val tripId: UUID)
    data class SaveCardRequest(val cardNumber: String, val expMonth: Int, val expYear: Int, val cvc: String)

    @PostMapping
    fun returnBikeAndSummarize(@RequestBody body: ReturnRequest): ReturnAndSummaryResponse {
        val trip = tripFacade.completeTripAndFetchDomain(body.tripId, body.destStationId, body.dockId)
        val summary = billing.summarize(trip)
        val rider = users.findById(trip.riderId).orElseThrow()
        val requiresPayment = payments.requiresImmediatePayment(rider) && summary.cost.totalCents > 0
        val saved = payments.getSavedCard(rider.id!!)
        return ReturnAndSummaryResponse(
            summary = summary,
            paymentStrategy = rider.paymentStrategy.name,
            requiresImmediatePayment = requiresPayment,
            hasSavedCard = saved.hasSavedCard,
            savedCardLast4 = saved.last4,
            provider = saved.provider
        )
    }

    @PostMapping("/save-card/{userId}")
    fun saveCard(@PathVariable userId: UUID, @RequestBody req: SaveCardRequest)
            = payments.saveCard(users.findById(userId).orElseThrow(), req.cardNumber, req.expMonth, req.expYear, req.cvc)

    @PostMapping("/charge/{userId}")
    fun charge(@PathVariable userId: UUID, @RequestBody req: ChargeRequest) =
        payments.handlePayment(users.findById(userId).orElseThrow(),
            billing.summarize(tripFacade.getTripDomain(req.tripId))
        )

    @PostMapping("/upgrade/{userId}")
    fun upgrade(@PathVariable userId: UUID) {
        val u = users.findById(userId).orElseThrow()
        u.paymentStrategy = PaymentStrategyType.MONTHLY_SUBSCRIPTION
        u.hasActiveSubscription = true
        users.save(u)
    }
}
