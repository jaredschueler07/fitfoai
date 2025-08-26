package com.runningcoach.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.runningcoach.v2.presentation.components.AppBottomNavigation
import com.runningcoach.v2.presentation.navigation.Screen
import com.runningcoach.v2.presentation.screen.aicoach.AICoachScreen
import com.runningcoach.v2.presentation.screen.connectapps.ConnectAppsScreen
import com.runningcoach.v2.presentation.screen.dashboard.DashboardScreen
import com.runningcoach.v2.presentation.screen.goal.SetEventGoalScreen
import com.runningcoach.v2.presentation.screen.profile.PersonalizeProfileScreen
import com.runningcoach.v2.presentation.screen.welcome.WelcomeScreen
import com.runningcoach.v2.presentation.screen.runtracking.RunTrackingScreen
import com.runningcoach.v2.presentation.theme.RunningCoachTheme
// import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint - Temporarily disabled
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine which screens should show bottom navigation
    val screensWithBottomNav = listOf(
        Screen.Dashboard.route,
        Screen.AICoach.route,
        Screen.Progress.route,
        Screen.Profile.route
    )
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in screensWithBottomNav) {
                AppBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Onboarding Flow
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
                        navController.navigate(Screen.PersonalizeProfile.route)
                    }
                )
            }
            
            composable(Screen.PersonalizeProfile.route) {
                PersonalizeProfileScreen(
                    onComplete = { profileData ->
                        navController.navigate(Screen.SetEventGoal.route)
                    }
                )
            }
            
            composable(Screen.SetEventGoal.route) {
                SetEventGoalScreen(
                    onComplete = { raceGoal ->
                        // Navigate to dashboard after onboarding is complete
                        navController.navigate(Screen.Dashboard.route) {
                            // Clear the onboarding stack
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Main App Screens (with bottom navigation)
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    userName = "Jane", // In real app, get from user data
                    onStartRun = {
                        navController.navigate(Screen.RunTracking.route)
                    }
                )
            }
            
            composable(Screen.AICoach.route) {
                AICoachScreen()
            }
            
            composable(Screen.Progress.route) {
                // Placeholder for now - will implement in Phase 3
                DashboardScreen(userName = "Progress View Coming Soon")
            }
            
            composable(Screen.Profile.route) {
                // Placeholder for now - will implement in Phase 3
                DashboardScreen(userName = "Profile Settings Coming Soon")
            }
            
            // Run Tracking Screen
            composable(Screen.RunTracking.route) {
                RunTrackingScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
