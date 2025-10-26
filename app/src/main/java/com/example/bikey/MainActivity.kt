// This is the app entry point, and it sets the Compose content and a small NavHost (with startDestination = "register")
// It immediately shows the Registration Screen (RegisterScreen)
package com.example.bikey

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bikey.ui.registration.RegisterScreen
import com.example.bikey.ui.login.LoginScreen
import com.example.bikey.ui.loading.LoadingScreen
import com.example.bikey.ui.theme.BiKeyTheme
import com.example.bikey.ui.welcome.WelcomeScreen
import com.example.bikey.ui.pricing.PricingScreen
import com.example.bikey.ui.operator.OperatorDashboardScreen
import com.example.bikey.ui.operator.OperatorMapDashboardScreen
import com.example.bikey.ui.rider.RiderDashboardScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "my_channel_id",
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

            setContent {
            BiKeyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val nav = rememberNavController()
                    NavHost(
                        navController = nav,
                        startDestination = "welcome"
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                onGetStarted = { nav.navigate("register") },
                                onLogin = { nav.navigate("login") },
                                onViewPricing = { nav.navigate("pricing") }
                            )
                        }

                        composable("pricing") {
                            PricingScreen(
                                onBack = { nav.popBackStack() },
                                onSelectPlan = { nav.navigate("register") }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegistered = { email ->
                                    // Navigate to loading screen first, then to dashboard
                                    nav.navigate("loadingToRider/$email") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onGoToLogin = {
                                    nav.navigate("login")
                                },
                                onGoBack = {
                                    nav.navigate("welcome") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("login") {
                            LoginScreen(
                                onGoToRegister = {
                                    nav.navigate("register")
                                },
                                onGoBack = {
                                    nav.navigate("welcome") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onLoggedIn = { email, role ->
                                    // Navigate to loading screen first, then to appropriate dashboard
                                    if (role == "OPERATOR") {
                                        nav.navigate("loadingToOperator/$email") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    } else {
                                        nav.navigate("loadingToRider/$email") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // Loading screen for rider dashboard
                        composable("loadingToRider/{email}") { backStackEntry ->
                            val email = backStackEntry.arguments?.getString("email") ?: ""
                            LoadingScreen(
                                message = "Welcome to BiKey!",
                                onLoadingComplete = {
                                    nav.navigate("riderDashboard/$email") {
                                        popUpTo("loadingToRider/$email") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Loading screen for operator dashboard
                        composable("loadingToOperator/{email}") { backStackEntry ->
                            val email = backStackEntry.arguments?.getString("email") ?: ""
                            LoadingScreen(
                                message = "Setting up operator dashboard...",
                                onLoadingComplete = {
                                    nav.navigate("operatorDashboard/$email") {
                                        popUpTo("loadingToOperator/$email") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("riderDashboard/{email}") { backStackEntry ->
                            val email = backStackEntry.arguments?.getString("email") ?: ""
                            RiderDashboardScreen(
                                riderEmail = email,
                                onLogout = {
                                    nav.navigate("welcome") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("operatorDashboard/{email}") { backStackEntry ->
                            val email = backStackEntry.arguments?.getString("email") ?: ""
                            OperatorDashboardScreen(
                                operatorEmail = email,
                                onLogout = {
                                    nav.navigate("welcome") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onNavigateToMapDashboard = {
                                    nav.navigate("operatorMapDashboard")
                                }
                            )
                        }

                        composable("operatorMapDashboard") {
                            OperatorMapDashboardScreen(
                                onNavigateBack = {
                                    nav.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
