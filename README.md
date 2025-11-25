## Biking-System

# biKey

### Backend
#### Overview
A kotlin/spring boot backend for managing the bike sharing system. It provides functionality 
for docking stations & docking logic, bikes (regular & electric), pricing & billing, payments & payment strategies,
trips & trip summaries, and notifications (email, push, SMS stubs).
The backend is implemented in Kotlin, using Spring Boot, JPA, and SQLite.
#### Requirements 
- Java 21
- Kotlin 2.x
- Gradle
- Spring Boot 3.3+
- Android Studio or IntelliJ IDEA
### Running the backend
At the project root, run:
```bash./gradlew :backend:bootRun
```
The backend will start on http://localhost:8080.

### Frontend
#### Overview
An Android app built with Jetpack Compose that allows users to register, log in, view available bikes, start and end trips, and view trip history.
The app communicates with the backend via RESTful APIs.
#### Requirements
- Java 21
- Kotlin 2.x
- Gradle
- Spring Boot 3.3+
- Android Studio or IntelliJ IDEA
- An open emulator or physical device running Android 8.0 (API level 26) or higher.
#### Running the frontend
At the project root, run:
```bash./gradlew :frontend:installDebug
```
Then launch the app on your emulator or device.

### Testing
To run all tests, execute:
```bash./gradlew :backend:test
```


### Additional Information
#### Registration Workflow
1. The app starts → MainActivity shows the NavHost with "register".
2. RegisterScreen renders fields from RegisterViewModel.state.
3. User types → screen calls onEmailChange/onPasswordChange → ViewModel updates state.
4. User clicks Register → submit():
5. Sets isLoading = true
6. Calls authApi.register(RegisterRequest(email, password))
7. Updates state based on HTTP result.
8. RegisterScreen sees new state:
9. Shows a spinner, an error, or calls onRegistered(email) on success.
10. onRegistered can navigate to another screen (e.g., "home"), once you add it.
#### Members 
- Justyne Phan
- Marwa Hammani
- Mugisha Samuel Rugomwa
- Suriya Paramathypathy
- Jingnan Wang
- Michael Pouget

