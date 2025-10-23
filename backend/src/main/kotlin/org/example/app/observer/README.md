 Observer Pattern (Bike System)

This folder contains our implementation of the Observer design pattern for the Bike System project.



Structure

Main Interfaces:
Observer → Anything that wants to receive updates.
Notifier → Anything that sends updates to observers.
Notifier Classes:
OvertimeNotifier → Checks if a bike is used for more than 45 minutes.
ReservationExpiryNotifier → Warns when a reservation is about to expire.
TripEndingNotifier → Sends a message when a trip is completed.
Observer Classes:
AppObserver → Sends live app notifications (using SSE).
EmailObserver → Sends an email to the user.
MessageTextObserver → Sends an SMS text message.

How It’s Used
We used to have a NotificationService to handle everything, but now the DockingStation or Bicycle classes can call the notifiers directly.
Here’s how it used to look:
notificationService.notifyOvertime("bike-123", 50)
notificationService.notifyReservationExpiry("bike-456", 2)
notificationService.notifyTripEnding("bike-789", "station-001", 25)


But now, we usually just call the right notifier directly after something happens (like returning a bike).

API Endpoints (for app & backend)
GET /api/notifications/stream/{userId} → Real-time notifications via SSE
POST /api/notifications/email/{userId} → Register email
POST /api/notifications/phone/{userId} → Register phone number
GET /api/notifications/stats → Get notification stats

 ndroid App Setup
The Android app connects to the server to get real-time updates:
val notificationManager = NotificationManager(context)
notificationManager.startNotificationService(userId)


Configuration
Eail (in application.properties):
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${MAIL_PASSWORD:your-app-password}

Android Dependencies (in app/build.gradle.kts):
implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")





