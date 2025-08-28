package com.runningcoach.v2.presentation.screen.runtracking

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.presentation.theme.AppColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.icons.*
import com.runningcoach.v2.presentation.components.maps.RunTrackingMap
// import com.runningcoach.v2.presentation.components.PermissionDialog
import com.google.android.gms.maps.model.LatLng

@Composable
fun RunTrackingScreen(
    viewModel: RunTrackingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        // Request location permissions when screen loads
        if (!uiState.hasLocationPermission && !uiState.permissionRequested) {
            viewModel.requestLocationPermissions()
        }
    }
    
    // Handle errors with snackbar or dialog
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error and clear it
            // In real app, show snackbar here
            viewModel.clearError()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.GradientStart,
                        AppColors.GradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Athletic Style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    ChevronRightIcon(tint = Color.White)
                }
                
                Text(
                    text = "RUN TRACKING",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { /* Settings */ }) {
                    SettingsIcon(tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main Metrics Display with Athletic Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.Surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Distance (main metric) with coral accent
                    Text(
                        text = uiState.formattedDistance.split(" ")[0],
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                        color = AppColors.CoralAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "miles",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricCard(
                            value = uiState.formattedDuration,
                            label = "Duration",
                            icon = Icons.Default.PlayArrow
                        )
                        MetricCard(
                            value = if (uiState.currentMetrics.currentPace > 0) {
                                // Convert pace from min/km to min/mile for display
                                val pacePerMile = uiState.currentMetrics.currentPace / 0.621371f
                                val minutes = pacePerMile.toInt()
                                val seconds = ((pacePerMile - minutes) * 60).toInt()
                                "$minutes:${seconds.toString().padStart(2, '0')}"
                            } else "--:--",
                            label = "Pace (min/mi)",
                            icon = Icons.Default.Add
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricCard(
                            value = uiState.formattedSpeed,
                            label = "Speed",
                            icon = Icons.Default.ArrowForward
                        )
                        MetricCard(
                            value = "${uiState.currentMetrics.caloriesBurned}",
                            label = "Calories",
                            icon = Icons.Default.Star
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // GPS Status Card with Athletic Styling and Background Tracking
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.Surface.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val gpsColor = when (uiState.gpsStatus) {
                                GPSStatus.EXCELLENT -> AppColors.GPSExcellent
                                GPSStatus.GOOD -> AppColors.GPSGood
                                GPSStatus.FAIR -> AppColors.GPSFair
                                GPSStatus.POOR -> AppColors.GPSPoor
                                GPSStatus.SIGNAL_LOST -> AppColors.Error
                                else -> AppColors.GPSInactive
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(gpsColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GPS: ${uiState.gpsStatus.name.replace("_", " ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                if (uiState.locationPointCount > 0) {
                                    Text(
                                        text = "${uiState.locationPointCount} points tracked",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        
                        // Background tracking indicator - temporarily disabled
                        // if (uiState.trackingState != TrackingState.INACTIVE) {
                        //     BackgroundTrackingIndicator(
                        //         isBackgroundServiceActive = uiState.isBackgroundServiceActive ?: false
                        //     )
                        // }
                    }
                    
                    // Service status if active - temporarily disabled
                    val isBackgroundServiceActive = false // TODO: Connect to real state
                    if (isBackgroundServiceActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Background Active",
                                tint = AppColors.Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Background tracking active",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Success,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Battery optimization warning if needed - temporarily disabled
                    val showBatteryOptimizationWarning = false // TODO: Connect to real state
                    if (showBatteryOptimizationWarning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.Warning.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Battery Warning",
                                    tint = AppColors.Warning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Battery optimization may affect tracking",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tap to optimize settings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Optimize",
                                    tint = AppColors.Warning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    // Session recovery notification - temporarily disabled
                    val hasRecoverableSession = false // TODO: Connect to real state
                    if (hasRecoverableSession) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.Info.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Recovery Available",
                                    tint = AppColors.Info,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Previous session found",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tap to recover your workout",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                TextButton(
                                    onClick = { /* Handle session recovery */ },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = AppColors.CoralAccent
                                    )
                                ) {
                                    Text(
                                        text = "Recover",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Map View for Route Tracking
            if (uiState.trackingState != TrackingState.INACTIVE) {
                // Convert location history to LatLng points
                val routePoints = uiState.locationHistory.map { locationData ->
                    LatLng(locationData.latitude, locationData.longitude)
                }
                
                val currentLocation = uiState.currentMetrics.currentLocation?.let { location ->
                    LatLng(location.latitude, location.longitude)
                }
                
                RunTrackingMap(
                    currentLocation = currentLocation,
                    routePoints = routePoints,
                    runMetrics = uiState.currentMetrics,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Athletic Control Buttons
            when {
                uiState.showStartButton -> {
                    CircularStartButton(
                        onClick = { viewModel.startRunSession() },
                        isLoading = uiState.isLoading
                    )
                }
                uiState.showPauseButton -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CircularActionButton(
                            onClick = { viewModel.pauseRunSession() },
                            icon = Icons.Default.Settings,
                            label = "Pause",
                            backgroundColor = AppColors.CoralAccentSecondary
                        )
                        CircularActionButton(
                            onClick = { viewModel.endRunSession() },
                            icon = Icons.Default.Close,
                            label = "Stop",
                            backgroundColor = AppColors.Error
                        )
                    }
                }
                uiState.showResumeButton -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CircularActionButton(
                            onClick = { viewModel.resumeRunSession() },
                            icon = Icons.Default.PlayArrow,
                            label = "Resume",
                            backgroundColor = AppColors.CoralAccent
                        )
                        CircularActionButton(
                            onClick = { viewModel.endRunSession() },
                            icon = Icons.Default.Close,
                            label = "Stop",
                            backgroundColor = AppColors.Error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DeepBlue.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AppColors.CoralAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CircularStartButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            containerColor = AppColors.CoralAccent,
            contentColor = Color.White
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Run",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "START",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.size(80.dp),
            shape = CircleShape,
            containerColor = backgroundColor,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BackgroundTrackingIndicator(
    isBackgroundServiceActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "backgroundPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isBackgroundServiceActive) {
                        AppColors.Success.copy(alpha = alpha)
                    } else {
                        AppColors.Neutral500
                    }
                )
        )
        
        Text(
            text = if (isBackgroundServiceActive) "BG" else "FG",
            style = MaterialTheme.typography.labelSmall,
            color = if (isBackgroundServiceActive) {
                AppColors.Success.copy(alpha = alpha)
            } else {
                AppColors.Neutral500
            },
            fontWeight = FontWeight.Bold
        )
    }
}

