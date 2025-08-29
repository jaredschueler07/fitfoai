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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import com.runningcoach.v2.data.service.APIConnectionManager
import com.runningcoach.v2.data.service.SpotifyService
import com.runningcoach.v2.domain.model.AppType
import com.runningcoach.v2.domain.model.ConnectedApp
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.PrimaryButton
import com.runningcoach.v2.presentation.components.SecondaryButton
import com.runningcoach.v2.presentation.components.ErrorSnackbar
import com.runningcoach.v2.presentation.components.SuccessSnackbar
import com.runningcoach.v2.presentation.components.icons.ChevronRightIcon
import com.runningcoach.v2.presentation.components.icons.GoogleFitIcon
import com.runningcoach.v2.presentation.components.icons.SpotifyIcon
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun ConnectAppsScreen(
    modifier: Modifier = Modifier,
    onComplete: (List<ConnectedApp>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Services
    val apiConnectionManager = remember { APIConnectionManager(context) }
    val spotifyService = remember { SpotifyService(context) }

    var connectedApps by remember { mutableStateOf(emptyList<ConnectedApp>()) }
    var connectingApp by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Observe connection states
    val googleFitConnected by apiConnectionManager.googleFitConnected.collectAsState()
    val spotifyConnected by spotifyService.isConnected.collectAsState()

    // Activity result launcher for Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)

            scope.launch {
                try {
                    manager.handleGoogleSignInResult()
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        manager.requestGoogleFitPermissions(activity)
                        successMessage = "Google Fit connected successfully!"
                    } else {
                        errorMessage = "Unable to request fitness permissions."
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to complete Google Fit connection."
                } finally {
                    connectingApp = null
                }
            }
        } catch (e: ApiException) {
            connectingApp = null
            errorMessage = "Google Sign-In failed. Please try again."
        }
    }

    // Update connectedApps list based on observed states
    LaunchedEffect(googleFitConnected, spotifyConnected) {
        val apps = mutableListOf<ConnectedApp>()
        if (googleFitConnected) {
            apps.add(ConnectedApp(id = "google_fit", name = "Google Fit", type = AppType.GOOGLE_FIT, isConnected = true))
        }
        if (spotifyConnected) {
            apps.add(ConnectedApp(id = "spotify", name = "Spotify", type = AppType.SPOTIFY, isConnected = true))
        }
        connectedApps = apps

        // Clear connecting state on successful connection
        if (googleFitConnected && connectingApp == "google_fit") connectingApp = null
        if (spotifyConnected && connectingApp == "spotify") connectingApp = null
    }

    val availableApps = listOf(
        ConnectedApp(id = "google_fit", name = "Google Fit", type = AppType.GOOGLE_FIT),
        ConnectedApp(id = "spotify", name = "Spotify", type = AppType.SPOTIFY)
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
            text = "Connect your fitness and music apps to personalize your training experience.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Neutral400,
            modifier = Modifier.padding(bottom = 32.dp)
        )

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
                    onToggleConnection = {
                        when (it.id) {
                            "google_fit" -> {
                                if (googleFitConnected) {
                                    apiConnectionManager.disconnectGoogleFit()
                                } else {
                                    connectingApp = "google_fit"
                                    val intent = apiConnectionManager.connectGoogleFit()
                                    googleSignInLauncher.launch(intent)
                                }
                            }
                            "spotify" -> {
                                if (spotifyConnected) {
                                    spotifyService.disconnect()
                                } else {
                                    connectingApp = "spotify"
                                    spotifyService.connect()
                                }
                            }
                        }
                    }
                )
            }
        }

        // Bottom buttons
        Column(modifier = Modifier.padding(top = 24.dp)) {
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

        // Snackbars
        ErrorSnackbar(
            message = errorMessage ?: "",
            isVisible = errorMessage != null,
            onDismiss = { errorMessage = null }
        )
        SuccessSnackbar(
            message = successMessage ?: "",
            isVisible = successMessage != null,
            onDismiss = { successMessage = null }
        )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App icon
                when (app.type) {
                    AppType.GOOGLE_FIT -> GoogleFitIcon(tint = if (isConnected) AppColors.Primary else AppColors.OnSurface)
                    AppType.SPOTIFY -> SpotifyIcon(tint = if (isConnected) AppColors.Primary else AppColors.OnSurface)
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
            ChevronRightIcon(tint = if (isConnected) AppColors.Primary else AppColors.Neutral500)
        }
    }
}