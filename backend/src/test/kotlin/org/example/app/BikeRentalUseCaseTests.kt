package org.example.app

import org.example.app.bmscoreandstationcontrol.api.ReserveBikeController
import org.example.app.bmscoreandstationcontrol.api.TakeBikeController
import org.example.app.bmscoreandstationcontrol.domain.*
import org.example.app.user.*
import org.example.app.bmscoreandstationcontrol.persistence.*
import org.example.app.billing.BillingService
import org.example.app.billing.TripSummaryDTO
import org.example.app.pricingandpayment.api.ReturnController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.*

class BikeRentalUseCaseTests {

    private lateinit var stationRepo: DockingStationRepository
    private lateinit var userRepo: UserRepository
    private lateinit var tripRepo: TripRepository
    private lateinit var bikeRepo: BicycleRepository
    private lateinit var addressRepo: AddressRepository
    private lateinit var stationSvc: DockingStationService
    private lateinit var billingService: BillingService
    private lateinit var paymentService: org.example.app.pricingandpayment.PaymentService
    private lateinit var tripFacade: org.example.app.pricingandpayment.api.TripFacade
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var loyaltyService: org.example.app.loyalty.LoyaltyService

    private lateinit var reserveController: ReserveBikeController
    private lateinit var takeController: TakeBikeController
    private lateinit var returnController: ReturnController

    private lateinit var testUser: User
    private lateinit var testStation: DockingStation
    private lateinit var testBike: Bike
    private lateinit var testStationB: DockingStation
    private lateinit var userAddress: Address
    private lateinit var addressA: Address
    private lateinit var addressB: Address

    @BeforeEach
    fun setup() {
        // Mocks
        stationRepo = mock()
        userRepo = mock()
        tripRepo = mock()
        bikeRepo = mock()
        addressRepo = mock()
        stationSvc = mock()
        billingService = mock()
        paymentService = mock()
        tripFacade = mock()
        eventPublisher = mock()
        loyaltyService = mock()

        // Controllers
        reserveController = ReserveBikeController(stationRepo, userRepo, stationSvc, loyaltyService)
        takeController = TakeBikeController(stationRepo, userRepo, stationSvc, tripRepo)
        returnController = ReturnController(
            userRepo, bikeRepo, billingService, paymentService, tripFacade, eventPublisher, stationRepo
        )

        // Test data IDs
        val userId = UUID.randomUUID()
        val stationId = UUID.randomUUID()
        val bikeId = UUID.randomUUID()
        val stationBId = UUID.randomUUID()

        // Test addresses
        userAddress = Address(UUID.randomUUID(), "1 Test St", null, "Montreal", Province.QC, "H1A1A1", "CA")
        addressA = Address(UUID.randomUUID(), "123 Main St", null, "Montreal", Province.QC, "H1A 1A1", "CA")
        addressB = Address(UUID.randomUUID(), "456 Oak Ave", null, "Montreal", Province.QC, "H2B 2B2", "CA")

        // Test user
        testUser = User(
            id = userId,
            email = "test@example.com",
            passwordHash = "hash",
            firstName = "Test",
            lastName = "User",
            username = "testuser",
            role = UserRole.RIDER,
            paymentStrategy = PaymentStrategyType.DEFAULT_PAY_NOW,
            flexDollars = 0.0f,
            address = userAddress
        )

        // Test bike
        testBike = Bike(bikeId).apply { status = BikeState.AVAILABLE }

        // Station A
        testStation = DockingStation(
            id = stationId,
            name = "Station A",
            address = addressA,
            location = LatLng(45.5017, -73.5673),
            status = null,
            capacity = 10,
            numFreeDocks = 8,
            numOccupiedDocks = 2,
            aBikeIsReserved = false,
            reservationHoldTime = Duration.ofMinutes(15),
            stateChanges = mutableListOf(),
            dashboard = Dashboard(),
            docks = mutableListOf(
                Dock(UUID.randomUUID(), testBike, DockState.OCCUPIED),
                Dock(UUID.randomUUID(), Bike(UUID.randomUUID()), DockState.OCCUPIED),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY)
            ),
            reservationUserId = null
        )

        // Station B
        testStationB = DockingStation(
            id = stationBId,
            name = "Station B",
            address = addressB,
            location = LatLng(45.5088, -73.5878),
            status = null,
            capacity = 10,
            numFreeDocks = 5,
            numOccupiedDocks = 5,
            aBikeIsReserved = false,
            reservationHoldTime = Duration.ofMinutes(15),
            stateChanges = mutableListOf(),
            dashboard = Dashboard(),
            docks = mutableListOf(
                Dock(UUID.randomUUID(), Bike(UUID.randomUUID()), DockState.OCCUPIED),
                Dock(UUID.randomUUID(), Bike(UUID.randomUUID()), DockState.OCCUPIED),
                Dock(UUID.randomUUID(), Bike(UUID.randomUUID()), DockState.OCCUPIED),
                Dock(UUID.randomUUID(), Bike(UUID.randomUUID()), DockState.OCCUPIED),
                Dock(UUID.randomUUID(), Bike(UUID.randomUUID()), DockState.OCCUPIED),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY)
            ),
            reservationUserId = null
        )

        // SAFE stubbing in @BeforeEach — no raw any()!
        whenever(stationRepo.findById(testStation.id)).thenReturn(Optional.of(DockingStationEntity(testStation)))
        whenever(stationRepo.findById(testStationB.id)).thenReturn(Optional.of(DockingStationEntity(testStationB)))
        whenever(stationRepo.save(any<DockingStationEntity>())).thenAnswer { it.arguments[0] as DockingStationEntity }

        whenever(addressRepo.findById(any<UUID>())).thenAnswer {
            when (it.arguments[0]) {
                userAddress.id -> Optional.of(userAddress)
                addressA.id -> Optional.of(addressA)
                addressB.id -> Optional.of(addressB)
                else -> Optional.empty()
            }
        }

        whenever(userRepo.findById(testUser.id!!)).thenReturn(Optional.of(testUser))
        whenever(userRepo.findByEmail(testUser.email)).thenReturn(testUser)
    }

    @Test
    fun `test happy path - reserve unlock ride return bill`() {
        // Step 1: Reserve
        val reserveRequest = ReserveBikeController.ReserveBikeRequest(
            stationId = testStation.id.toString(),
            bikeId = testBike.id.toString(),
            userId = testUser.id.toString()
        )

        whenever(stationSvc.reserveBike(any(), any(), any())).thenReturn(Unit)

        val reserveResponse = reserveController.reserveBike(reserveRequest)
        assertNotNull(reserveResponse)
        assertEquals(testBike.id.toString(), reserveResponse.bikeId)

        verify(stationSvc).reserveBike(any(), any(), any())
        verify(stationRepo).save(any<DockingStationEntity>())

        // Step 2: Take (unlock)
        val takeRequest = TakeBikeController.TakeBikeRequest(
            stationId = testStation.id.toString(),
            userEmail = testUser.email
        )

        testBike.status = BikeState.RESERVED
        testStation.aBikeIsReserved = true
        testStation.reservationUserId = testUser.id

        whenever(stationSvc.takeBike(any(), any(), any(), any())).thenReturn(Unit)

        val takeResponse = takeController.takeBike(takeRequest)
        assertNotNull(takeResponse)
        assertEquals(testBike.id.toString(), takeResponse.bikeId)
        assertNotNull(takeResponse.tripId)

        val tripId = UUID.fromString(takeResponse.tripId!!)

        // Step 3: Return
        val savedTrip = Trip(
            id = tripId,
            riderId = testUser.id!!,
            bikeId = testBike.id,
            startStationId = testStation.id,
            destStationId = null,
            startedAt = Instant.now().minusSeconds(1800),
            endedAt = null,
            status = org.example.app.bmscoreandstationcontrol.persistence.TripStatus.IN_PROGRESS
        )
        whenever(tripRepo.findById(tripId)).thenReturn(Optional.of(savedTrip))
        whenever(tripRepo.save(any<Trip>())).thenAnswer { it.arguments[0] as Trip }

        val tripDomain = BillingService.TripDomain(
            id = tripId,
            riderId = testUser.id!!,
            bikeId = testBike.id,
            startStationName = "Station A",
            endStationName = "Station B",
            startTime = Instant.now().minusSeconds(1800),
            endTime = Instant.now(),
            isEBike = false,
            overtimeCents = 0
        )

        // THIS IS THE KEY FIX
        whenever(tripFacade.completeTripAndFetchDomain(
            tripId = eq(tripId),
            destStationId = eq(testStationB.id),
            dockId = isNull()
        )).thenReturn(tripDomain)

        val tripSummary = TripSummaryDTO(
            tripId = tripId,
            riderId = testUser.id!!,
            bikeId = testBike.id,
            startStationName = "Station A",
            endStationName = "Station B",
            startTime = tripDomain.startTime,
            endTime = tripDomain.endTime,
            durationMinutes = 30,
            isEBike = false,
            cost = org.example.app.billing.CostBreakdownDTO(
                baseCents = 100,
                perMinuteCents = 0,
                minutes = 30,
                eBikeSurchargeCents = null,
                overtimeCents = null,
                discountCents = 0,
                loyaltyTier = null,
                flexDollarCents = 0,
                totalCents = 100
            )
        )

        whenever(billingService.summarize(any(), any(), any(), any())).thenReturn(tripSummary)
        whenever(paymentService.requiresImmediatePayment(any())).thenReturn(true)
        whenever(paymentService.getSavedCard(any())).thenReturn(
            org.example.app.pricingandpayment.PaymentService.SavedCardView(false, null, null)
        )
        whenever(userRepo.save(any<User>())).thenAnswer { it.arguments[0] as User }

        val returnRequest = ReturnController.ReturnRequest(
            tripId = tripId.toString(),
            destStationId = testStationB.id.toString(),
            dockId = null,
            distanceTravelled = 5
        )

        val returnResponse = returnController.returnBikeAndSummarize(returnRequest)

        assertNotNull(returnResponse)
        assertEquals("Station A", returnResponse.summary.startStationName)
        assertEquals("Station B", returnResponse.summary.endStationName)
        assertEquals(30, returnResponse.summary.durationMinutes)
        assertEquals(100, returnResponse.summary.cost.totalCents)

        verify(billingService).summarize(any(), any(), any(), any())
        verify(userRepo).save(any<User>())
    }


    /**
     * Test Case 2: Station Full - Return attempt at full station is rejected
     */
    @Test
    fun `test station full - return rejects bike`() {
        // Setup: Station B is completely full (all docks occupied)
        testStationB.numOccupiedDocks = 10
        testStationB.numFreeDocks = 0
        testStationB.docks = (1..10).map {
            Dock(
                UUID.randomUUID(),
                Bike(UUID.randomUUID(), BikeState.AVAILABLE),
                DockState.OCCUPIED
            )
        }.toMutableList()
        testStationB.status = Full(testStationB)

        // Create a trip
        val tripId = UUID.randomUUID()

        val returnRequest = ReturnController.ReturnRequest(
            tripId = tripId.toString(),
            destStationId = testStationB.id.toString(),
            dockId = null,
            distanceTravelled = 5
        )

        // Station B is full - no available docks
        val stationBEntity = DockingStationEntity(testStationB)
        whenever(stationRepo.findById(testStationB.id)).thenReturn(Optional.of(stationBEntity))

        // Mock bike repository
        val bikeEntity = BicycleEntity(testBike)
        whenever(bikeRepo.findById(testBike.id)).thenReturn(Optional.of(bikeEntity))

        // The tripFacade.completeTripAndFetchDomain should throw an exception or fail
        // when trying to dock at a full station
        whenever(tripFacade.completeTripAndFetchDomain(any(), any(), any())).thenThrow(
            IllegalStateException("No available docks at station")
        )

        // Expect the return to fail
        val exception = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            returnController.returnBikeAndSummarize(returnRequest)
        }

        // Verify the error is an internal server error (caught by the generic catch block)
        assertEquals(500, exception.statusCode.value())
        assertTrue(exception.reason?.contains("Internal error") == true)
    }

    /**
     * Test Case 3: Reservation Expiry - Reservation holding time lapses; bike state changes to available
     */
    @Test
    fun `test reservation expiry - bike becomes available after expiry`() {
        // Setup: Bike is reserved
        testBike.status = BikeState.RESERVED
        testBike.reservationExpiryTime = Instant.now().minusSeconds(60) // Expired 1 minute ago
        testStation.aBikeIsReserved = true
        testStation.reservationUserId = testUser.id

        // Update the dock to contain the reserved bike
        val reservedDock = testStation.docks.first { it.bike?.id == testBike.id }
        reservedDock.bike = testBike
        reservedDock.status = DockState.OCCUPIED

        // Call updateReservation which should check expiry
        val result = testStation.updateReservation()

        // Verify reservation was cleared
        assertNotNull(result, "updateReservation should return ok")
        assertFalse(testStation.aBikeIsReserved, "Reservation flag should be cleared")
        assertEquals(BikeState.AVAILABLE, testBike.status, "Bike should be available after expiry")
        assertNull(testBike.reservationExpiryTime, "Expiry time should be cleared")
    }

    /**
     * Test Case 4: Rebalancing - Station is emptied → operator gets an alert
     */
    @Test
    fun `test rebalancing - station emptied triggers state change to empty`() {
        // Setup: Station has one bike (testBike already in first dock from setup)
        testStation.numOccupiedDocks = 1
        testStation.numFreeDocks = 9
        testStation.docks = mutableListOf(
            Dock(UUID.randomUUID(), testBike, DockState.OCCUPIED),
            *((1..9).map { Dock(UUID.randomUUID(), null, DockState.EMPTY) }.toTypedArray())
        )
        testStation.status = PartiallyFilled(testStation)

        // Simulate taking the last bike (operator moving it)
        val result = testStation.status?.takeBike(testBike, false, testUser.id, userRepo)

        // Verify take was successful
        assertNotNull(result, "Take should succeed")

        // Verify station status changed to Empty
        assertTrue(testStation.status is Empty, "Station should be empty after last bike is taken")
        assertEquals(0, testStation.numOccupiedDocks, "Station should have 0 occupied docks")
        assertEquals(10, testStation.numFreeDocks, "Station should have all docks free")

        // Note: In a real implementation, there would be a notifier/observer pattern
        // that alerts operators when station becomes empty. This would be tested separately.
    }
}