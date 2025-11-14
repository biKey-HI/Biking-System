package org.example.app.pricingandpayment.api

import org.example.app.billing.BillingService
import org.example.app.billing.ReturnAndSummaryResponse
import org.example.app.bmscoreandstationcontrol.persistence.BicycleRepository
import org.example.app.bmscoreandstationcontrol.persistence.DockingStationRepository
import org.example.app.pricingandpayment.PaymentService
import org.example.app.user.PaymentStrategyType
import org.example.app.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.util.UUID

@RestController
@RequestMapping("/api/return")
class ReturnController(
    private val users: UserRepository,
    private val bikes: BicycleRepository,
    private val billing: BillingService,
    private val payments: PaymentService,
    private val tripFacade: TripFacade,
    private val stations: DockingStationRepository
) {

    // debug
    private val logger = LoggerFactory.getLogger(ReturnController::class.java)

    // NOTE: Accept IDs as strings from the client, parse explicitly below
    data class ReturnRequest(val tripId: String, val destStationId: String, val dockId: String?)
    data class ChargeRequest(val tripId: String)
    data class SaveCardRequest(val cardNumber: String, val expMonth: Int, val expYear: Int, val cvc: String)

    @PostMapping
    fun returnBikeAndSummarize(@RequestBody body: ReturnRequest): ReturnAndSummaryResponse {
        logger.info("Received return request: $body")
        // parse UUIDs explicitly to provide clearer errors
        val tripUuid = try {
            UUID.fromString(body.tripId)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid tripId format: ${body.tripId}")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tripId")
        }
        val destStationUuid = try {
            UUID.fromString(body.destStationId)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid destStationId format: ${body.destStationId}")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid destStationId")
        }

        try {
            val station = stations.findById(destStationUuid).orElseThrow {
                logger.warn("Station not found for id=${destStationUuid}")
                ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found")
            }
            val receivesFlexDollars = station.offersFlexDollars()
            LoggerFactory.getLogger("ReceivesFlexDollars").info("Flex Dollars Received: ${receivesFlexDollars}")

            val trip = try {
                tripFacade.completeTripAndFetchDomain(tripUuid, destStationUuid, body.dockId)
            } catch (e: NoSuchElementException) {
                logger.warn("Trip not found or cannot complete trip: $tripUuid", e)
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found or cannot complete")
            }

            val rider = users.findById(trip.riderId).orElseThrow {
                logger.warn("Rider not found for id=${trip.riderId}")
                ResponseStatusException(HttpStatus.NOT_FOUND, "Rider not found")
            }

            val summary = billing.summarize(trip, bikes, rider.paymentStrategy, rider)

            val requiresPayment = payments.requiresImmediatePayment(rider) && summary.cost.totalCents > 0
            val saved = payments.getSavedCard(rider.id!!)

            LoggerFactory.getLogger("UsedFlexDollars").info("Flex Dollars Applied: ${summary.cost.flexDollarCents.toFloat()/100.0}")

            rider.flexDollars += (if(receivesFlexDollars) 0.25 else 0.0).toFloat()
            users.save(rider)

            LoggerFactory.getLogger("NewFlexDollars").info("Flex Dollars Updated: ${rider.flexDollars}")

            return ReturnAndSummaryResponse(
                summary = summary,
                paymentStrategy = rider.paymentStrategy.name,
                requiresImmediatePayment = requiresPayment,
                hasSavedCard = saved.hasSavedCard,
                savedCardLast4 = saved.last4,
                provider = saved.provider
            )
        } catch (e: ResponseStatusException) {
            // rethrow to preserve status/message
            throw e
        } catch (e: Exception) {
            logger.error("Error processing return request for trip=${body.tripId}", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error while processing return")
        }
    }

    @PostMapping("/save-card/{userId}")
    fun saveCard(@PathVariable userId: UUID, @RequestBody req: SaveCardRequest)
            = payments.saveCard(users.findById(userId).orElseThrow(), req.cardNumber, req.expMonth, req.expYear, req.cvc)

    @PostMapping("/charge/{userId}")
    fun charge(@PathVariable userId: UUID, @RequestBody req: ChargeRequest) =
        // parse trip id string -> UUID for internal facade
        try {
            val tripUuid = UUID.fromString(req.tripId)
            val user = users.findById(userId).orElseThrow()
            payments.handlePayment(
                user,
                billing.summarize(tripFacade.getTripDomain(tripUuid), bikes, user.paymentStrategy, user)
            )
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tripId")
        }

    @PostMapping("/upgrade/{userId}")
    fun upgrade(@PathVariable userId: UUID) {
        val u = users.findById(userId).orElseThrow()
        u.paymentStrategy = PaymentStrategyType.MONTHLY_SUBSCRIPTION
        u.hasActiveSubscription = true
        users.save(u)
    }
}
