package com.runningcoach.v2.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.presentation.theme.AppColors
import kotlinx.coroutines.delay

enum class VoiceStatus {
    INACTIVE,
    PREPARING,
    GENERATING,
    SPEAKING,
    ERROR,
    API_LIMIT_REACHED,
    NETWORK_ERROR,
    CACHE_HIT
}

data class VoiceStatusData(
    val status: VoiceStatus,
    val queuedMessages: Int = 0,
    val cacheHitCount: Int = 0,
    val errorMessage: String? = null,
    val isCoachingEnabled: Boolean = true,
    val currentCoach: String? = null
)

@Composable
fun VoiceStatusIndicator(
    statusData: VoiceStatusData,
    onClick: (() -> Unit)? = null,
    showDetails: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isClickable = onClick != null
    
    Card(
        modifier = modifier
            .then(
                if (isClickable) {
                    Modifier.clickable { onClick?.invoke() }
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status indicator with animation
            VoiceStatusIcon(status = statusData.status)
            
            if (showDetails) {
                // Status details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getStatusTitle(statusData.status),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = getStatusDescription(statusData),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
                
                // Queue indicator if relevant
                if (statusData.queuedMessages > 0) {
                    VoiceQueueIndicator(
                        queueCount = statusData.queuedMessages
                    )
                }
            }
        }
    }
}

@Composable
fun CompactVoiceStatusIndicator(
    statusData: VoiceStatusData,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isClickable = onClick != null
    
    Box(
        modifier = modifier
            .size(48.dp)
            .then(
                if (isClickable) {
                    Modifier.clickable { onClick?.invoke() }
                } else Modifier
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        getStatusColor(statusData.status),
                        getStatusColor(statusData.status).copy(alpha = 0.7f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        VoiceStatusIcon(
            status = statusData.status,
            size = 24.dp
        )
        
        // Queue badge
        if (statusData.queuedMessages > 0) {
            Badge(
                modifier = Modifier.align(Alignment.TopEnd),
                containerColor = AppColors.CoralAccent
            ) {
                Text(
                    text = statusData.queuedMessages.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VoiceStatusBar(
    statusData: VoiceStatusData,
    onToggleCoaching: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VoiceStatusIndicator(
                    statusData = statusData,
                    showDetails = false,
                    modifier = Modifier.width(120.dp)
                )
                
                Column {
                    Text(
                        text = if (statusData.isCoachingEnabled) "Voice Coaching Active" else "Voice Coaching Paused",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    statusData.currentCoach?.let { coach ->
                        Text(
                            text = "Coach: $coach",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle coaching
                IconButton(
                    onClick = onToggleCoaching,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (statusData.isCoachingEnabled) {
                                AppColors.Success.copy(alpha = 0.2f)
                            } else {
                                AppColors.Error.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (statusData.isCoachingEnabled) {
                            Icons.Default.VolumeUp
                        } else {
                            Icons.Default.VolumeOff
                        },
                        contentDescription = if (statusData.isCoachingEnabled) {
                            "Mute Coaching"
                        } else {
                            "Enable Coaching"
                        },
                        tint = if (statusData.isCoachingEnabled) {
                            AppColors.Success
                        } else {
                            AppColors.Error
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Settings
                IconButton(
                    onClick = onShowSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Voice Settings",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceStatusIcon(
    status: VoiceStatus,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusAnimation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (status == VoiceStatus.GENERATING || status == VoiceStatus.PREPARING) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = if (status == VoiceStatus.SPEAKING) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = if (status == VoiceStatus.SPEAKING) 1f else 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Icon(
        imageVector = getStatusIcon(status),
        contentDescription = getStatusTitle(status),
        tint = getStatusColor(status).copy(alpha = alpha),
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
private fun VoiceQueueIndicator(
    queueCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CoralAccent.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Queue,
                contentDescription = "Queued Messages",
                tint = AppColors.CoralAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = queueCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.CoralAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VoiceErrorDialog(
    errorStatus: VoiceStatus,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    isVisible: Boolean = true
) {
    if (isVisible && (errorStatus == VoiceStatus.ERROR || errorStatus == VoiceStatus.API_LIMIT_REACHED || errorStatus == VoiceStatus.NETWORK_ERROR)) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = AppColors.Error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = when (errorStatus) {
                        VoiceStatus.API_LIMIT_REACHED -> "Voice Limit Reached"
                        VoiceStatus.NETWORK_ERROR -> "Connection Issue"
                        else -> "Voice Error"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = when (errorStatus) {
                            VoiceStatus.API_LIMIT_REACHED -> 
                                "You've reached the daily voice generation limit. Voice coaching will resume tomorrow, or you can upgrade for unlimited coaching."
                            VoiceStatus.NETWORK_ERROR -> 
                                "Unable to connect to voice services. Check your internet connection and try again."
                            else -> 
                                errorMessage ?: "An unexpected error occurred with voice coaching."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (errorStatus == VoiceStatus.API_LIMIT_REACHED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.Info.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = "💡 Tip: Cached messages will still play for common coaching scenarios.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Info,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (errorStatus != VoiceStatus.API_LIMIT_REACHED) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.CoralAccent
                        )
                    ) {
                        Text("Retry")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            },
            containerColor = AppColors.DeepBlue,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

private fun getStatusIcon(status: VoiceStatus) = when (status) {
    VoiceStatus.INACTIVE -> Icons.Default.VolumeOff
    VoiceStatus.PREPARING -> Icons.Default.Refresh
    VoiceStatus.GENERATING -> Icons.Default.AutoAwesome
    VoiceStatus.SPEAKING -> Icons.Default.VolumeUp
    VoiceStatus.ERROR -> Icons.Default.ErrorOutline
    VoiceStatus.API_LIMIT_REACHED -> Icons.Default.Warning
    VoiceStatus.NETWORK_ERROR -> Icons.Default.SignalWifiOff
    VoiceStatus.CACHE_HIT -> Icons.Default.Storage
}

private fun getStatusColor(status: VoiceStatus) = when (status) {
    VoiceStatus.INACTIVE -> AppColors.Neutral500
    VoiceStatus.PREPARING -> AppColors.Warning
    VoiceStatus.GENERATING -> AppColors.Info
    VoiceStatus.SPEAKING -> AppColors.Success
    VoiceStatus.ERROR, VoiceStatus.NETWORK_ERROR -> AppColors.Error
    VoiceStatus.API_LIMIT_REACHED -> AppColors.Warning
    VoiceStatus.CACHE_HIT -> AppColors.CoralAccent
}

private fun getStatusTitle(status: VoiceStatus) = when (status) {
    VoiceStatus.INACTIVE -> "Voice Inactive"
    VoiceStatus.PREPARING -> "Preparing..."
    VoiceStatus.GENERATING -> "Generating Voice"
    VoiceStatus.SPEAKING -> "Speaking"
    VoiceStatus.ERROR -> "Voice Error"
    VoiceStatus.API_LIMIT_REACHED -> "Daily Limit Reached"
    VoiceStatus.NETWORK_ERROR -> "Connection Issue"
    VoiceStatus.CACHE_HIT -> "Using Cached Voice"
}

private fun getStatusDescription(statusData: VoiceStatusData) = when (statusData.status) {
    VoiceStatus.INACTIVE -> "Voice coaching is disabled"
    VoiceStatus.PREPARING -> "Analyzing your performance..."
    VoiceStatus.GENERATING -> "Creating personalized coaching message"
    VoiceStatus.SPEAKING -> "Providing real-time guidance"
    VoiceStatus.ERROR -> statusData.errorMessage ?: "Unknown error occurred"
    VoiceStatus.API_LIMIT_REACHED -> "Switch to cached messages or upgrade"
    VoiceStatus.NETWORK_ERROR -> "Check internet connection"
    VoiceStatus.CACHE_HIT -> "Playing from local cache (${statusData.cacheHitCount} cached)"
}