package com.runningcoach.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.runningcoach.v2.presentation.navigation.Screen
import com.runningcoach.v2.presentation.screen.connectapps.ConnectAppsScreen
import com.runningcoach.v2.presentation.screen.welcome.WelcomeScreen
import com.runningcoach.v2.presentation.theme.RunningCoachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunningCoachTheme {
                RunningCoachApp()
            }
        }
    }
}

@Composable
fun RunningCoachApp() {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onGetStarted = {
                        navController.navigate(Screen.ConnectApps.route)
                    }
                )
            }
            
            composable(Screen.ConnectApps.route) {
                ConnectAppsScreen(
                    onComplete = { connectedApps ->
                        // For now, navigate to a placeholder screen
                        // Later we'll add PersonalizeProfile screen
                        navController.navigate(Screen.PersonalizeProfile.route)
                    }
                )
            }
            
            composable(Screen.PersonalizeProfile.route) {
                // Placeholder screen for now
                WelcomeScreen(
                    onGetStarted = {
                        navController.navigate(Screen.Welcome.route)
                    }
                )
            }
        }
    }
}
