# Unit Test Scenarios for Bike Rental System

This document describes the unit test scenarios that exercise the interactive use cases for the bike rental system.

## Test File: `BikeRentalUseCaseTests.kt`

### Test Scenarios

#### 1. Happy Path: Reserve → Unlock → Ride → Return → Bill
**Test Method:** `test happy path - reserve unlock ride return bill`

**Scenario:**
- Rider reserves a bike at Station A
- Rider unlocks the bike (takes it from the station)
- Rider rides the bike
- Rider returns the bike at Station B
- System computes and outputs the bill with trip information

**What is tested:**
- ✅ Bike reservation process
- ✅ Bike unlock/take process
- ✅ Trip creation
- ✅ Bike return process
- ✅ Bill computation with trip summary
- ✅ Station state transitions

**Expected Results:**
- Reservation response contains bike ID and expiry time
- Take bike response contains trip ID and start time
- Return response contains complete trip summary with:
  - Start and end station names
  - Duration
  - Cost breakdown
  - Total cost

---

#### 2. Station Full: Return at Full Station Triggers Flex Dollars Credit
**Test Method:** `test station full - return triggers flex dollars credit`

**Scenario:**
- Rider returns a bike to a station that is full (all docks occupied)
- System detects the station is at capacity (< 25% free)
- System awards 0.25 flex dollars to the rider's account
- User account balance is updated

**What is tested:**
- ✅ Station full detection logic (`offersFlexDollars()`)
- ✅ Flex dollars credit calculation (0.25 when station < 25% free)
- ✅ User account balance update
- ✅ Return process at full station

**Expected Results:**
- Station is detected as offering flex dollars
- User's flex dollars balance increases by 0.25
- Return completes successfully
- User account is saved with updated balance

---

#### 3. Reservation Expiry: Bike Becomes Available After Expiry
**Test Method:** `test reservation expiry - bike becomes available after expiry`

**Scenario:**
- A bike is reserved by a rider
- Reservation hold time expires (e.g., 15 minutes pass)
- System automatically updates reservation status
- Bike state changes from RESERVED to AVAILABLE
- Reservation flag is cleared

**What is tested:**
- ✅ Reservation expiry detection
- ✅ Automatic state transition (RESERVED → AVAILABLE)
- ✅ Reservation flag clearing
- ✅ Expiry time cleanup

**Expected Results:**
- `updateReservation()` returns success
- Bike status changes to AVAILABLE
- `aBikeIsReserved` flag is set to false
- `reservationExpiryTime` is cleared

---

#### 4. Rebalancing: Station Emptied Triggers Operator Alert
**Test Method:** `test rebalancing - station emptied triggers operator alert`

**Scenario:**
- Station has one bike remaining
- Last bike is taken (station becomes empty)
- Station status changes to Empty
- System should notify operators (in real implementation)

**What is tested:**
- ✅ Station state transition to Empty
- ✅ Dock state updates
- ✅ Station capacity tracking
- ✅ Empty state detection

**Expected Results:**
- Station status becomes `Empty`
- `numOccupiedDocks` becomes 0
- `numFreeDocks` equals station capacity
- All docks are in EMPTY state

**Note:** In a full implementation, this would also test operator notification via the observer pattern (Notificator, Emailer, etc.)

---

## Running the Tests

To run these tests, use:

```bash
cd backend
./gradlew test
```

Or run a specific test:

```bash
./gradlew test --tests "BikeRentalUseCaseTests"
```

## Test Dependencies

The tests use:
- **JUnit 5** for test framework
- **Mockito** for mocking dependencies
- **Mockito Kotlin** for Kotlin-friendly mocking

## Mocking Strategy

The tests mock:
- `DockingStationRepository` - Station persistence
- `UserRepository` - User persistence
- `TripRepository` - Trip persistence
- `BicycleRepository` - Bike persistence
- `DockingStationService` - Station business logic
- `BillingService` - Billing calculations
- `PaymentService` - Payment processing
- `TripFacade` - Trip domain operations
- `ApplicationEventPublisher` - Event publishing

## Future Enhancements

1. **Integration Tests**: Add integration tests that test the full flow with a real database
2. **Operator Notification Tests**: Add tests for the notification system when stations become empty
3. **Edge Cases**: Add tests for:
   - Multiple concurrent reservations
   - Reservation conflicts
   - Payment failures
   - Network errors
4. **Performance Tests**: Test system behavior under load

