package com.runningcoach.v2.presentation.screen.permissions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runningcoach.v2.data.service.HealthConnectPermissionManager
import com.runningcoach.v2.presentation.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun HealthConnectPermissionScreen(
    onPermissionsGranted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthConnectPermissionViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.checkAvailability()
    }
    
    // Handle navigation based on permission state
    LaunchedEffect(uiState.hasRequiredPermissions) {
        if (uiState.hasRequiredPermissions) {
            onPermissionsGranted()
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header
            Text(
                text = "Health Connect",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Connect with your health ecosystem",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Content based on state
            when (uiState.availability) {
                HealthConnectAvailability.CHECKING -> {
                    LoadingState()
                }
                HealthConnectAvailability.AVAILABLE -> {
                    if (uiState.hasRequiredPermissions) {
                        PermissionGrantedState()
                    } else {
                        PermissionRequestState(
                            onGrantPermissions = { viewModel.requestPermissions() },
                            onSkip = onSkip
                        )
                    }
                }
                HealthConnectAvailability.NEEDS_UPDATE -> {
                    UpdateRequiredState(
                        onUpdate = { viewModel.openPlayStoreForUpdate() },
                        onSkip = onSkip
                    )
                }
                HealthConnectAvailability.UNAVAILABLE -> {
                    UnavailableState(onContinue = onSkip)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    HealthConnectStepCard(
        title = "Checking Health Connect",
        description = "We're checking if Health Connect is available on your device...",
        icon = Icons.Default.Refresh,
        modifier = modifier
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = AppColors.CoralAccent
        )
    }
}

@Composable
private fun PermissionRequestState(
    onGrantPermissions: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    HealthConnectStepCard(
        title = "Connect to Health Connect",
        description = "Health Connect lets FITFOAI sync with your health apps while keeping your data secure and private.",
        icon = Icons.Default.HealthAndSafety,
        benefits = listOf(
            "Sync fitness data across all health apps",
            "Automatically backup your running history", 
            "Enhanced AI coaching with health insights",
            "Better battery life and performance",
            "Advanced privacy controls"
        ),
        buttonText = "Connect to Health Connect",
        onButtonClick = onGrantPermissions,
        onSkip = onSkip,
        modifier = modifier
    )
}

@Composable
private fun PermissionGrantedState(modifier: Modifier = Modifier) {
    HealthConnectStepCard(
        title = "Health Connect Ready!",
        description = "Your fitness data will now sync seamlessly across your health apps.",
        icon = Icons.Default.CheckCircle,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AppColors.CoralAccent,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Successfully connected",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.CoralAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UpdateRequiredState(
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    HealthConnectStepCard(
        title = "Update Health Connect",
        description = "Health Connect needs to be updated to work with FITFOAI. Please update from the Play Store.",
        icon = Icons.Default.Update,
        benefits = listOf(
            "Latest health data features",
            "Improved security and privacy",
            "Better app compatibility", 
            "Enhanced performance"
        ),
        buttonText = "Update Health Connect",
        onButtonClick = onUpdate,
        onSkip = onSkip,
        modifier = modifier
    )
}

@Composable
private fun UnavailableState(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    HealthConnectStepCard(
        title = "Health Connect Not Available",
        description = "Health Connect isn't available on this device. FITFOAI will work with local storage instead.",
        icon = Icons.Default.CloudOff,
        benefits = listOf(
            "All core features still available",
            "Local data storage and sync",
            "Manual backup and export options",
            "Full running tracking capabilities"
        ),
        buttonText = "Continue Without Health Connect",
        onButtonClick = onContinue,
        modifier = modifier
    )
}

@Composable
private fun HealthConnectStepCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    benefits: List<String>? = null,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AppColors.CoralAccent,
                                AppColors.CoralAccentSecondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = AppColors.DeepBlue,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.DeepBlue.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            // Benefits
            if (benefits != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    benefits.forEach { benefit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AppColors.CoralAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.DeepBlue.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
            
            // Custom content
            if (content != null) {
                Spacer(modifier = Modifier.height(24.dp))
                content()
            }
            
            // Action buttons
            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onButtonClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.CoralAccent
                    )
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Skip button for optional flow
                if (onSkip != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip for now",
                            style = MaterialTheme.typography.titleSmall,
                            color = AppColors.DeepBlue.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// Enums and data classes for UI state
enum class HealthConnectAvailability {
    CHECKING,
    AVAILABLE,
    NEEDS_UPDATE,
    UNAVAILABLE
}

data class HealthConnectPermissionUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.CHECKING,
    val hasRequiredPermissions: Boolean = false,
    val hasOptionalPermissions: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)