# Biking-System

### Registration Workflow
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