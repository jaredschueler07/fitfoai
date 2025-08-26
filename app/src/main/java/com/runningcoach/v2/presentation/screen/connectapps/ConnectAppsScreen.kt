package com.runningcoach.v2.presentation.screen.connectapps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    onComplete: (List<ConnectedApp>) -> Unit,
    modifier: Modifier = Modifier
) {
    var connectedApps by remember { mutableStateOf(emptyList<ConnectedApp>()) }
    
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
        
        // Apps list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(availableApps) { app ->
                AppConnectionCard(
                    app = app,
                    isConnected = connectedApps.any { it.id == app.id },
                    onToggleConnection = { toggleApp ->
                        connectedApps = if (connectedApps.any { it.id == toggleApp.id }) {
                            connectedApps.filter { it.id != toggleApp.id }
                        } else {
                            connectedApps + toggleApp.copy(isConnected = true)
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
    onToggleConnection: (ConnectedApp) -> Unit,
    modifier: Modifier = Modifier
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
                        text = if (isConnected) "Connected" else "Tap to connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) AppColors.Primary else AppColors.Neutral400
                    )
                }
            }
            
            ChevronRightIcon(
                tint = if (isConnected) AppColors.Primary else AppColors.Neutral500
            )
        }
    }
}
