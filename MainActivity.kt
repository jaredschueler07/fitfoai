package com.runningcoach.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.runningcoach.v2.presentation.navigation.Screen
import com.runningcoach.v2.presentation.screen.aicoach.AICoachScreen
import com.runningcoach.v2.presentation.screen.connectapps.ConnectAppsScreen
import com.runningcoach.v2.presentation.screen.dashboard.DashboardScreen
import com.runningcoach.v2.presentation.screen.goal.SetEventGoalScreen
import com.runningcoach.v2.presentation.screen.permissions.PermissionScreen
import com.runningcoach.v2.presentation.screen.profile.PersonalizeProfileScreen
import com.runningcoach.v2.presentation.screen.progress.ProgressScreen
import com.runningcoach.v2.presentation.screen.settings.SettingsScreen
import com.runningcoach.v2.presentation.screen.runtracking.RunTrackingScreen
import com.runningcoach.v2.presentation.screen.welcome.WelcomeScreen
import com.runningcoach.v2.presentation.theme.RunningCoachV2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunningCoachV2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RunningCoachApp()
                }
            }
        }
    }
}

@Composable
fun RunningCoachApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.WelcomeScreen.route) {
        composable(Screen.WelcomeScreen.route) {
            WelcomeScreen(navController = navController) // Placeholder
        }
        composable(Screen.ConnectAppsScreen.route) {
            ConnectAppsScreen(navController = navController) // Placeholder
        }
        composable(Screen.PersonalizeProfileScreen.route) {
            PersonalizeProfileScreen(navController = navController) // Placeholder
        }
        composable(Screen.SetEventGoalScreen.route) {
            SetEventGoalScreen(navController = navController) // Placeholder
        }
        composable(Screen.DashboardScreen.route) {
            DashboardScreen(navController = navController) // Placeholder
        }
        composable(Screen.AICoachScreen.route) {
            AICoachScreen(navController = navController) // Placeholder
        }
        composable(Screen.ProgressScreen.route) {
            ProgressScreen(navController = navController) // Placeholder
        }
        composable(Screen.ProfileScreen.route) {
            //ProfileScreen(navController = navController) // Placeholder - Assuming ProfileScreen exists
        }
        composable(Screen.SettingsScreen.route) {
            SettingsScreen(navController = navController) // Placeholder
        }
        composable(Screen.RunTrackingScreen.route) {
            RunTrackingScreen(navController = navController) // Placeholder
        }
        // Add more composable destinations for other screens as they are created
    }
}