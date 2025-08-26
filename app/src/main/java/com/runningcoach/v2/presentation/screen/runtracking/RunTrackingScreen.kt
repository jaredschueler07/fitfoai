package com.runningcoach.v2.presentation.screen.runtracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.presentation.theme.AppColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.icons.*

@Composable
fun RunTrackingScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTracking by remember { mutableStateOf(false) }
    var distance by remember { mutableStateOf("0.00") }
    var duration by remember { mutableStateOf("00:00") }
    var pace by remember { mutableStateOf("--:--") }
    var speed by remember { mutableStateOf("0.0") }
    var calories by remember { mutableStateOf("0") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                         IconButton(onClick = onNavigateBack) {
                 ChevronRightIcon(tint = AppColors.OnSurface)
             }
            
            Text(
                text = "Run Tracking",
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { /* Settings */ }) {
                SettingsIcon(tint = AppColors.OnSurface)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main metrics display
        AppCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Distance (main metric)
                Text(
                    text = distance,
                    style = MaterialTheme.typography.displayLarge,
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "kilometers",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.Neutral500
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Secondary metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem(
                        value = duration,
                        label = "Duration",
                        icon = { ClockIcon(tint = AppColors.Primary) }
                    )
                    MetricItem(
                        value = pace,
                        label = "Pace",
                        icon = { SpeedIcon(tint = AppColors.Primary) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem(
                        value = speed,
                        label = "Speed",
                        icon = { SpeedIcon(tint = AppColors.Primary) }
                    )
                    MetricItem(
                        value = calories,
                        label = "Calories",
                        icon = { FireIcon(tint = AppColors.Primary) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // GPS Status
        AppCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTracking) Color.Green else Color.Red
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isTracking) "GPS Active" else "GPS Inactive",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (!isTracking) {
                Button(
                    onClick = { isTracking = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlayIcon(tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Run")
                    }
                }
            } else {
                Button(
                    onClick = { isTracking = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PauseIcon(tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause")
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = { 
                        isTracking = false
                        // Reset metrics
                        distance = "0.00"
                        duration = "00:00"
                        pace = "--:--"
                        speed = "0.0"
                        calories = "0"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        StopIcon(tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    value: String,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.OnSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Neutral500,
            textAlign = TextAlign.Center
        )
    }
}

// Additional icons for run tracking
@Composable
fun ClockIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.OnSurface
) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Clock",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun SpeedIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.OnSurface
) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Speed",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun FireIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.OnSurface
) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Calories",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun PlayIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.OnSurface
) {
    Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = "Play",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun PauseIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.OnSurface
) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Pause",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun StopIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.OnSurface
) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Stop",
        modifier = modifier,
        tint = tint
    )
}
