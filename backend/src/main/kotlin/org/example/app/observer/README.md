# Observer Pattern Implementation

This directory contains the implementation of the Observer design pattern for the Bike System project.

## Overview

The Observer pattern allows objects (observers) to be notified of changes in another object (subject/notifier) without tight coupling. In our bike system, this enables real-time notifications for various events.

## Architecture

### Core Interfaces

- **`Observer`**: Interface for objects that want to be notified of changes
- **`Notifier`**: Interface for subjects that can be observed

### Concrete Notifiers (Subjects)

- **`OvertimeNotifier`**: Monitors bicycles for overtime usage (45+ minutes)
- **`ReservationExpiryNotifier`**: Monitors bicycle reservations for expiry warnings
- **`TripEndingNotifier`**: Monitors bicycle trips for completion events

### Concrete Observers

- **`AppObserver`**: Handles real-time notifications to mobile app via Server-Sent Events (SSE)
- **`EmailObserver`**: Sends email notifications to users
- **`MessageTextObserver`**: Sends SMS notifications to users

## Usage

### Backend Integration

The `NotificationService` coordinates all observers and notifiers:

```kotlin
@Autowired
private val notificationService: NotificationService

// Notify about overtime
notificationService.notifyOvertime("bike-123", 50)

// Notify about reservation expiry
notificationService.notifyReservationExpiry("bike-456", 2)

// Notify about trip ending
notificationService.notifyTripEnding("bike-789", "station-001", 25)
```

### REST API Endpoints

- `GET /api/notifications/stream/{userId}` - SSE endpoint for real-time notifications
- `POST /api/notifications/email/{userId}` - Register email for notifications
- `POST /api/notifications/phone/{userId}` - Register phone for SMS notifications
- `GET /api/notifications/stats` - Get notification statistics

### Android Integration

The Android app connects to the SSE endpoint for real-time notifications:

```kotlin
val notificationManager = NotificationManager(context)
notificationManager.startNotificationService(userId)
```

## Configuration

### Email Settings

Configure email settings in `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${MAIL_PASSWORD:your-app-password}
```

### Android Dependencies

Add to `app/build.gradle.kts`:

```kotlin
implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
```

## Testing

Run the observer pattern tests:

```bash
./gradlew test --tests ObserverPatternTest
```

## Benefits

1. **Loose Coupling**: Notifiers don't need to know about specific observers
2. **Extensibility**: Easy to add new notification types
3. **Real-time Updates**: SSE provides instant notifications to mobile apps
4. **Multiple Channels**: Support for app, email, and SMS notifications
5. **Scalability**: Background monitoring with scheduled tasks

## Future Enhancements

- Add push notifications for mobile apps
- Implement notification preferences per user
- Add notification history and analytics
- Integrate with external SMS services (Twilio, etc.)
- Add notification templates and localization
