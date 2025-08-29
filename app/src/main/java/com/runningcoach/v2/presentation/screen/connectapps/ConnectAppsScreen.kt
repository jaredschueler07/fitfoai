package com.runningcoach.v2.presentation.screen.connectapps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import com.runningcoach.v2.domain.model.AppType
import com.runningcoach.v2.domain.model.ConnectedApp
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.PrimaryButton
import com.runningcoach.v2.presentation.components.SecondaryButton
import com.runningcoach.v2.presentation.components.icons.ChevronRightIcon
import com.runningcoach.v2.presentation.components.icons.GoogleFitIcon
import com.runningcoach.v2.presentation.components.icons.SpotifyIcon
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun ConnectAppsScreen(
    modifier: Modifier = Modifier,
    onComplete: (List<ConnectedApp>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // API Connection Manager
    val apiConnectionManager = remember { 
        try {
            com.runningcoach.v2.data.service.APIConnectionManager(context)
        } catch (e: Exception) {
            android.util.Log.e("ConnectAppsScreen", "Error creating APIConnectionManager", e)
            null
        }
    }
    
    var connectedApps by remember { mutableStateOf(emptyList<ConnectedApp>()) }
    var connectingApp by remember { mutableStateOf<String?>(null) }
    
    // Observe connection states
    val googleFitConnected by (apiConnectionManager?.googleFitConnected ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val spotifyConnected by (apiConnectionManager?.spotifyConnected ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val connectionStatus by (apiConnectionManager?.connectionStatus ?: kotlinx.coroutines.flow.MutableStateFlow("Error: Connection manager not initialized")).collectAsState()
    
    // Activity result launcher for Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            
            android.util.Log.i("ConnectAppsScreen", "Google Sign-In successful: ${account?.email}")
            
            // Handle successful sign-in
            apiConnectionManager?.let { manager ->
                // Use coroutine scope to handle the async operations
                scope.launch {
                    try {
                        // Handle the Google Sign-In result and update connection status
                        manager.handleGoogleSignInResult()
                        // After sign-in, request Google Fit Fitness permissions if needed
                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            manager.requestGoogleFitPermissions(activity)
                        } else {
                            android.util.Log.w("ConnectAppsScreen", "Unable to request Fitness permissions: context is not an Activity")
                        }
                        
                        android.util.Log.i("ConnectAppsScreen", "Google Sign-In handling complete")
                    } catch (e: Exception) {
                        android.util.Log.e("ConnectAppsScreen", "Error handling Google Sign-In result", e)
                    } finally {
                        connectingApp = null
                    }
                }
            } ?: run {
                connectingApp = null
            }
            
        } catch (e: ApiException) {
            android.util.Log.e("ConnectAppsScreen", "Google Sign-In failed", e)
            connectingApp = null
        } catch (e: Exception) {
            android.util.Log.e("ConnectAppsScreen", "Unexpected error during Google Sign-In", e)
            connectingApp = null
        }
    }

    // Update connectedApps based on actual connection states
    LaunchedEffect(googleFitConnected, spotifyConnected) {
        val apps = mutableListOf<ConnectedApp>()
        if (googleFitConnected) {
            apps.add(ConnectedApp(
                id = "google_fit",
                name = "Google Fit",
                type = AppType.GOOGLE_FIT,
                isConnected = true
            ))
            // Clear connecting state when successfully connected
            if (connectingApp == "google_fit") {
                connectingApp = null
            }
        }
        if (spotifyConnected) {
            apps.add(ConnectedApp(
                id = "spotify",
                name = "Spotify",
                type = AppType.SPOTIFY,
                isConnected = true
            ))
            // Clear connecting state when successfully connected
            if (connectingApp == "spotify") {
                connectingApp = null
            }
        }
        connectedApps = apps
    }
    
    val availableApps = listOf(
        ConnectedApp(
            id = "google_fit",
            name = "Google Fit",
            type = AppType.GOOGLE_FIT
        ),
        ConnectedApp(
            id = "spotify",
            name = "Spotify",
            type = AppType.SPOTIFY
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Connect Your Apps",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnBackground,
            modifier = Modifier.padding(top = 40.dp, bottom = 8.dp)
        )
        
        Text(
            text = "Connect your fitness and music apps to personalize your training experience",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Neutral400,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Connection status message
        connectionStatus?.let { status ->
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = AppColors.Primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Apps list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(availableApps) { app ->
                AppConnectionCard(
                    app = app,
                    isConnected = when (app.id) {
                        "google_fit" -> googleFitConnected
                        "spotify" -> spotifyConnected
                        else -> false
                    },
                    isConnecting = connectingApp == app.id,
                    onToggleConnection = { toggleApp ->
                        when (toggleApp.id) {
                            "google_fit" -> {
                                apiConnectionManager?.let { manager ->
                                    if (googleFitConnected) {
                                        manager.disconnectGoogleFit()
                                    } else {
                                        connectingApp = "google_fit"
                                        try {
                                            // Start Google Fit connection
                                            val intent = manager.connectGoogleFit()
                                            if (intent.action != null || intent.component != null) {
                                                googleSignInLauncher.launch(intent)
                                            } else {
                                                // If no intent is returned, user might already be connected
                                                manager.testGoogleFitConnection()
                                                connectingApp = null
                                            }
                                        } catch (e: Exception) {
                                            // Handle any errors during connection
                                            android.util.Log.e("ConnectAppsScreen", "Error connecting to Google Fit", e)
                                            connectingApp = null
                                        }
                                    }
                                } ?: run {
                                    android.util.Log.e("ConnectAppsScreen", "APIConnectionManager is null")
                                }
                            }
                            "spotify" -> {
                                apiConnectionManager?.let { manager ->
                                    if (spotifyConnected) {
                                        manager.disconnectSpotify()
                                    } else {
                                        connectingApp = "spotify"
                                        try {
                                            // Start OAuth flow
                                            val intent = manager.connectSpotify()
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Handle any errors during connection
                                            android.util.Log.e("ConnectAppsScreen", "Error connecting to Spotify", e)
                                            connectingApp = null
                                        }
                                    }
                                } ?: run {
                                    android.util.Log.e("ConnectAppsScreen", "APIConnectionManager is null")
                                 }
                            }
                        }
                    }
                )
            }
        }
        
        // Bottom buttons
        Column(
            modifier = Modifier.padding(top = 24.dp)
        ) {
            if (connectedApps.isNotEmpty()) {
                PrimaryButton(
                    text = "Continue",
                    onClick = { onComplete(connectedApps) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            SecondaryButton(
                text = "Skip for now",
                onClick = { onComplete(emptyList()) }
            )
        }
    }
}

@Composable
private fun AppConnectionCard(
    app: ConnectedApp,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    isConnecting: Boolean = false,
    onToggleConnection: (ConnectedApp) -> Unit
) {
    AppCard(
        modifier = modifier,
        backgroundColor = if (isConnected) AppColors.Primary.copy(alpha = 0.1f) else AppColors.CardBackground,
        borderColor = if (isConnected) AppColors.Primary else AppColors.CardBorder,
        onClick = { onToggleConnection(app) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
                when (app.type) {
                    AppType.GOOGLE_FIT -> GoogleFitIcon(
                        tint = if (isConnected) AppColors.Primary else AppColors.OnSurface
                    )
                    AppType.SPOTIFY -> SpotifyIcon(
                        tint = if (isConnected) AppColors.Primary else AppColors.OnSurface
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnected) AppColors.Primary else AppColors.OnSurface
                    )
                    
                    Text(
                        text = when {
                            isConnected -> "Connected"
                            isConnecting -> "Connecting..."
                            else -> "Tap to connect"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isConnected -> AppColors.Primary
                            isConnecting -> AppColors.Primary.copy(alpha = 0.7f)
                            else -> AppColors.Neutral400
                        }
                    )
                }
            }
            
            ChevronRightIcon(
                tint = if (isConnected) AppColors.Primary else AppColors.Neutral500
            )
        }
    }
}
