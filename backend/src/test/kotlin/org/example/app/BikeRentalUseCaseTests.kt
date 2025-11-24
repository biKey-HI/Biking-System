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
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.argThat
import org.springframework.context.ApplicationEventPublisher
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

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
        stationRepo = mock(DockingStationRepository::class.java)
        userRepo = mock(UserRepository::class.java)
        tripRepo = mock(TripRepository::class.java)
        bikeRepo = mock(BicycleRepository::class.java)
        addressRepo = mock(AddressRepository::class.java)
        stationSvc = mock(DockingStationService::class.java)
        billingService = mock(BillingService::class.java)
        paymentService = mock(org.example.app.pricingandpayment.PaymentService::class.java)
        tripFacade = mock(org.example.app.pricingandpayment.api.TripFacade::class.java)
        eventPublisher = mock(ApplicationEventPublisher::class.java)
        loyaltyService = mock(org.example.app.loyalty.LoyaltyService::class.java)

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
        userAddress = Address(
            id = UUID.randomUUID(),
            line1 = "1 Test St",
            line2 = null,
            city = "Montreal",
            province = Province.QC,
            postalCode = "H1A1A1",
            country = "CA"
        )

        addressA = Address(
            id = UUID.randomUUID(),
            line1 = "123 Main St",
            line2 = null,
            city = "Montreal",
            province = Province.QC,
            postalCode = "H1A 1A1",
            country = "CA"
        )

        addressB = Address(
            id = UUID.randomUUID(),
            line1 = "456 Oak Ave",
            line2 = null,
            city = "Montreal",
            province = Province.QC,
            postalCode = "H2B 2B2",
            country = "CA"
        )

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
        testBike = Bike(bikeId)
        testBike.status = BikeState.AVAILABLE

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
            numFreeDocks = 9,
            numOccupiedDocks = 1,
            aBikeIsReserved = false,
            reservationHoldTime = Duration.ofMinutes(15),
            stateChanges = mutableListOf(),
            dashboard = Dashboard(),
            docks = mutableListOf(
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), null, DockState.EMPTY),
                Dock(UUID.randomUUID(), Bike(UUID.randomUUID()), DockState.OCCUPIED)
            ),
            reservationUserId = null
        )

        // Default stationRepo behaviour:
        // return corresponding DockingStationEntity when findById is called for either testStation or testStationB
        whenever(stationRepo.findById(testStation.id)).thenReturn(Optional.of(DockingStationEntity(testStation)))
        whenever(stationRepo.findById(testStationB.id)).thenReturn(Optional.of(DockingStationEntity(testStationB)))
        // Return the saved entity back when save(...) is called (prevents NPEs inside controller)
        whenever(stationRepo.save(any())).thenAnswer { invocation ->
            // assume save gets passed a DockingStationEntity; return it back
            invocation.getArgument(0) as DockingStationEntity
        }

        // Mock address repository
        whenever(addressRepo.findById(userAddress.id!!)).thenReturn(Optional.of(userAddress))
        whenever(addressRepo.findById(addressA.id!!)).thenReturn(Optional.of(addressA))
        whenever(addressRepo.findById(addressB.id!!)).thenReturn(Optional.of(addressB))

        // Default userRepo lookup
        whenever(userRepo.findById(testUser.id!!)).thenReturn(Optional.of(testUser))
        whenever(userRepo.findByEmail(testUser.email)).thenReturn(testUser)
    }

    /**
     * Test Case 1: Happy Path
     * Rider reserves at Station A, unlocks, rides, returns at Station B, bill computed
     */
    @Test
    fun `test happy path - reserve unlock ride return bill`() {
        // Step 1: Reserve bike at Station A
        val reserveRequest = ReserveBikeController.ReserveBikeRequest(
            stationId = testStation.id.toString(),
            bikeId = testBike.id.toString(),
            userId = testUser.id.toString()
        )

        // ensure controller can read station
        whenever(stationRepo.findById(testStation.id)).thenReturn(Optional.of(DockingStationEntity(testStation)))
        whenever(userRepo.findById(testUser.id!!)).thenReturn(Optional.of(testUser))
        whenever(stationSvc.reserveBike(any(), any(), any())).thenReturn(Unit)

        val reserveResponse = reserveController.reserveBike(reserveRequest)
        assertNotNull(reserveResponse)
        assertEquals(testBike.id.toString(), reserveResponse.bikeId)
        assertTrue(reserveResponse.reservedUntilEpochMs > Instant.now().toEpochMilli())

        // Verify bike is reserved
        verify(stationSvc).reserveBike(any(), any(), any())
        verify(stationRepo).save(any())

        // Step 2: Take bike (unlock)
        val takeRequest = TakeBikeController.TakeBikeRequest(
            stationId = testStation.id.toString(),
            userEmail = testUser.email
        )

        // Update station state - bike is now reserved
        testBike.status = BikeState.RESERVED
        testStation.aBikeIsReserved = true
        testStation.reservationUserId = testUser.id

        whenever(stationRepo.findById(testStation.id)).thenReturn(Optional.of(DockingStationEntity(testStation)))
        whenever(userRepo.findByEmail(testUser.email)).thenReturn(testUser)
        whenever(stationSvc.takeBike(any(), any(), any(), any())).thenReturn(Unit)

        val takeResponse = takeController.takeBike(takeRequest)
        assertNotNull(takeResponse)
        assertEquals(testBike.id.toString(), takeResponse.bikeId)
        assertNotNull(takeResponse.tripId)
        assertTrue(takeResponse.startedAtEpochMs > 0)

        // Verify trip was created
        verify(tripRepo).save(any())

        // Step 3: Return bike at Station B
        val tripId = UUID.fromString(takeResponse.tripId)
        val returnRequest = ReturnController.ReturnRequest(
            tripId = tripId.toString(),
            destStationId = testStationB.id.toString(),
            dockId = null,
            distanceTravelled = 5
        )

        // Setup trip domain for return
        val tripDomain = BillingService.TripDomain(
            id = tripId,
            riderId = testUser.id!!,
            bikeId = testBike.id,
            startStationName = "Station A",
            endStationName = "Station B",
            startTime = Instant.now().minusSeconds(1800), // 30 minutes ago
            endTime = Instant.now(),
            isEBike = false,
            overtimeCents = 0
        )

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

        // Controller dependencies for return
        whenever(stationRepo.findById(testStationB.id)).thenReturn(Optional.of(DockingStationEntity(testStationB)))
        whenever(tripFacade.completeTripAndFetchDomain(any(), any(), any())).thenReturn(tripDomain)
        whenever(billingService.summarize(any(), any(), any(), any())).thenReturn(tripSummary)
        whenever(paymentService.requiresImmediatePayment(any())).thenReturn(true)
        whenever(paymentService.getSavedCard(any())).thenReturn(
            org.example.app.pricingandpayment.PaymentService.SavedCardView(false, null, null)
        )
        whenever(userRepo.findById(testUser.id!!)).thenReturn(Optional.of(testUser))
        whenever(userRepo.save(any())).thenReturn(testUser)

        val returnResponse = returnController.returnBikeAndSummarize(returnRequest)
        assertNotNull(returnResponse)
        assertEquals("Station A", returnResponse.summary.startStationName)
        assertEquals("Station B", returnResponse.summary.endStationName)
        assertEquals(30, returnResponse.summary.durationMinutes)
        assertEquals(100, returnResponse.summary.cost.totalCents)

        // Verify billing was computed
        verify(billingService).summarize(any(), any(), any(), any())
        verify(userRepo).save(any())
    }

    /**
     * Test Case 2: Station Full - Return attempt at full station triggers overflow credit
     */
    @Test
    fun `test station full - return triggers flex dollars credit`() {
        // Setup: Station B is full (all docks occupied)
        testStationB.numOccupiedDocks = 10
        testStationB.numFreeDocks = 0
        testStationB.docks = (1..10).map {
            Dock(
                UUID.randomUUID(),
                Bike(UUID.randomUUID(), BikeState.AVAILABLE).apply { status = BikeState.AVAILABLE },
                DockState.OCCUPIED
            )
        }.toMutableList()
        testStationB.status = Full(testStationB)

        // Create a trip
        val tripId = UUID.randomUUID()
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

        val returnRequest = ReturnController.ReturnRequest(
            tripId = tripId.toString(),
            destStationId = testStationB.id.toString(),
            dockId = null,
            distanceTravelled = 5
        )

        whenever(stationRepo.findById(testStationB.id)).thenReturn(Optional.of(DockingStationEntity(testStationB)))
        whenever(tripFacade.completeTripAndFetchDomain(any(), any(), any())).thenReturn(tripDomain)
        whenever(billingService.summarize(any(), any(), any(), any())).thenReturn(tripSummary)
        whenever(paymentService.requiresImmediatePayment(any())).thenReturn(true)
        whenever(paymentService.getSavedCard(any())).thenReturn(
            org.example.app.pricingandpayment.PaymentService.SavedCardView(false, null, null)
        )

        // User starts with 0 flex dollars
        testUser.flexDollars = 0.0f
        whenever(userRepo.findById(testUser.id!!)).thenReturn(Optional.of(testUser))
        whenever(userRepo.save(any())).thenAnswer { invocation ->
            val savedUser = invocation.getArgument(0) as User
            // Simulate flex dollars being added
            assertEquals(0.25f, savedUser.flexDollars, "User should receive 0.25 flex dollars for returning to full station")
            savedUser
        }

        val returnResponse = returnController.returnBikeAndSummarize(returnRequest)

        // Verify flex dollars were added
        verify(userRepo).save(argThat { user: User ->
            user.flexDollars == 0.25f
        })
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
