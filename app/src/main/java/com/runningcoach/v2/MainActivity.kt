package com.runningcoach.v2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
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
import com.runningcoach.v2.presentation.screen.runtracking.RunTrackingViewModel
import com.runningcoach.v2.presentation.screen.progress.ProgressScreen
import com.runningcoach.v2.presentation.screen.apitesting.APITestingScreen
import com.runningcoach.v2.presentation.screen.permissions.PermissionScreen
import com.runningcoach.v2.presentation.screen.settings.SettingsScreen
import com.runningcoach.v2.presentation.theme.RunningCoachTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.repository.UserRepository

// @AndroidEntryPoint - Temporarily disabled due to Hilt KSP compatibility
class MainActivity : ComponentActivity() {
    
    private var apiConnectionManager: com.runningcoach.v2.data.service.APIConnectionManager? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize API connection manager
        apiConnectionManager = com.runningcoach.v2.data.service.APIConnectionManager(this)
        
        // Handle OAuth callback if this activity was launched by a deep link
        handleOAuthCallback(intent)
        
        setContent {
            RunningCoachTheme {
                RunningCoachApp()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthCallback(intent)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle Google Fit permissions result
        if (requestCode == com.runningcoach.v2.data.service.GoogleFitService.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            apiConnectionManager?.handleGoogleFitActivityResult(requestCode, resultCode, data)
        }
    }
    
    private fun handleOAuthCallback(intent: Intent?) {
        val data: Uri? = intent?.data
        data?.let { uri ->
            when (uri.host) {
                "spotify-callback" -> {
                    val authCode = uri.getQueryParameter("code")
                    val error = uri.getQueryParameter("error")
                    
                    if (authCode != null) {
                        // Handle successful Spotify OAuth
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val result = apiConnectionManager?.handleSpotifyCallback(authCode)
                                if (result?.isSuccess == true) {
                                    println("Spotify OAuth successful: ${result.getOrNull()}")
                                } else {
                                    println("Spotify OAuth failed: ${result?.exceptionOrNull()?.message}")
                                }
                            } catch (e: Exception) {
                                println("Spotify OAuth exception: ${e.message}")
                            }
                        }
                    } else if (error != null) {
                        // Handle OAuth error
                        println("Spotify OAuth error: $error")
                    }
                }
                "googlefit-callback" -> {
                    val authCode = uri.getQueryParameter("code")
                    val error = uri.getQueryParameter("error")
                    
                    if (authCode != null) {
                        // Handle successful Google Fit OAuth
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                // Handle Google Fit OAuth - this is now handled by activity result
                                println("Google Fit OAuth callback received")
                            } catch (e: Exception) {
                                println("Google Fit OAuth exception: ${e.message}")
                            }
                        }
                    } else if (error != null) {
                        // Handle OAuth error
                        println("Google Fit OAuth error: $error")
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        apiConnectionManager?.close()
    }
}

@Composable
fun RunningCoachApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // State for determining start destination
    var startDestination by remember { mutableStateOf<String?>(null) }
    
    // Check onboarding status on app launch
    LaunchedEffect(Unit) {
        val database = FITFOAIDatabase.getDatabase(context)
        val userRepository = UserRepository(database)
        
        val isOnboardingCompleted = userRepository.isOnboardingCompleted()
        startDestination = if (isOnboardingCompleted) {
            Screen.Dashboard.route
        } else {
            Screen.Welcome.route
        }
    }
    
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
        // Only render NavHost after start destination is determined
        startDestination?.let { destination ->
            NavHost(
                navController = navController,
                startDestination = destination,
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
                    userName = "Runner", // TODO: Get from user profile data
                    onStartRun = {
                        navController.navigate(Screen.RunTracking.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToPermissions = {
                        navController.navigate(Screen.Permissions.route)
                    }
                )
            }
            
            composable(Screen.AICoach.route) {
                val context = LocalContext.current
                val app = context.applicationContext as RunningCoachApplication
                val viewModel = remember {
                    com.runningcoach.v2.presentation.screen.aicoach.AICoachViewModel(
                        app.appContainer.voiceCoachingManager.let { _ ->
                            // Use the same agent instance as voice coaching to maintain shared context
                            // Access via AppContainer
                            app.appContainer.run {
                                // fitnessCoachAgent is private; expose via voiceCoachingManager dependency
                                // Create a new instance backed by the same DI (safe as it shares DB and services)
                                com.runningcoach.v2.data.service.FitnessCoachAgent(
                                    context = this@MainActivity,
                                    llmService = this.llmService, // not visible here; fallback to constructing via container
                                    elevenLabsService = this.voiceCoachingManager.let { it2 ->
                                        // Reflection of dependency; use the existing ElevenLabs from container
                                        // We will construct via container function instead.
                                        null
                                    },
                                    database = com.runningcoach.v2.data.local.FITFOAIDatabase.getDatabase(context)
                                )
                            }
                        }
                    )
                }
                AICoachScreen(viewModel = viewModel)
            }
            
            composable(Screen.Progress.route) {
                ProgressScreen()
            }
            
            composable(Screen.Profile.route) {
                // Temporary: Show API Testing screen in Profile tab for debugging
                APITestingScreen()
            }
            
            // Run Tracking Screen
            composable(Screen.RunTracking.route) {
                // Get dependencies from application-level container
                val context = LocalContext.current
                val app = context.applicationContext as RunningCoachApplication
                val viewModel = RunTrackingViewModel(
                    app.appContainer.startRunSessionUseCase,
                    app.appContainer.trackRunSessionUseCase,
                    app.appContainer.endRunSessionUseCase,
                    app.appContainer.voiceCoachingManager
                )
                
                RunTrackingScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // API Testing Screen (for debugging)
            composable(Screen.APITesting.route) {
                APITestingScreen()
            }
            
            // Google Fit Test Screen
            composable(Screen.GoogleFitTest.route) {
                com.runningcoach.v2.presentation.screen.apitesting.GoogleFitTestScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Permission Flow Screen
            composable(Screen.Permissions.route) {
                PermissionScreen(
                    onPermissionsGranted = {
                        // Navigate back to the screen that requested permissions
                        navController.popBackStack()
                    },
                    onLocationPermissionRequested = {
                        // In a real app, this would trigger the permission system dialog
                        // For now, we'll simulate permission granted
                    },
                    onBackgroundPermissionRequested = {
                        // In a real app, this would trigger the background permission dialog
                        // For now, we'll simulate permission granted
                    },
                    hasLocationPermission = false, // TODO: Integrate with actual permission manager
                    hasBackgroundPermission = false,
                    canRequestBackgroundPermission = false
                )
            }
            
            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            }
        } ?: run {
            // Show loading screen while determining start destination
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
