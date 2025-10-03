// This is the app entry point, and it sets the Compose content and a small NavHost (with startDestination = "register")
// It immediately shows the Registration Screen (RegisterScreen)
package com.example.bikey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bikey.ui.registration.RegisterScreen
import com.example.bikey.ui.login.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val nav = rememberNavController()
                    NavHost(
                        navController = nav,
                        startDestination = "register"
                    ) {
                        composable("register") {
                            RegisterScreen(
                                onRegistered = { email -> },
                                onGoToLogin = {
                                nav.navigate("login")
                            }
                            )
                        }
                        composable("login") {
                            LoginScreen(onLoggedIn = { email ->

                            })
                            LoginScreen(
                                onGoToRegister = {
                                    nav.navigate("register")
                                }
                            )


                        }
                        // composable("home") { HomeScreen() }
                    }
                }
            }
        }
    }
}


