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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onBackgroundPermissionRequested: () -> Unit,
    hasLocationPermission: Boolean = false,
    hasBackgroundPermission: Boolean = false,
    canRequestBackgroundPermission: Boolean = false,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(if (hasLocationPermission) 2 else 1) }
    
    // Update step based on permissions
    LaunchedEffect(hasLocationPermission, hasBackgroundPermission) {
        when {
            hasBackgroundPermission -> {
                // All permissions granted, navigate to next screen
                onPermissionsGranted()
            }
            hasLocationPermission && canRequestBackgroundPermission -> {
                currentStep = 2
            }
            hasLocationPermission -> {
                currentStep = 3 // Battery optimization step
            }
            else -> {
                currentStep = 1
            }
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
                text = "Location Permissions",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Enable precise GPS tracking for your runs",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Progress indicator
            PermissionProgressIndicator(
                currentStep = currentStep,
                totalSteps = 3
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Step content with animation
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "StepAnimation"
            ) { step ->
                when (step) {
                    1 -> LocationPermissionStep(
                        onContinue = onLocationPermissionRequested
                    )
                    2 -> BackgroundPermissionStep(
                        onContinue = onBackgroundPermissionRequested
                    )
                    3 -> BatteryOptimizationStep(
                        onContinue = onPermissionsGranted
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val stepNumber = index + 1
            val isActive = stepNumber <= currentStep
            val isCompleted = stepNumber < currentStep
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isActive) AppColors.CoralAccent 
                        else Color.White.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            if (stepNumber < currentStep) AppColors.CoralAccent
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun LocationPermissionStep(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionStepCard(
        title = "Enable Location Access",
        description = "FITFO AI needs location access to track your runs with GPS precision.",
        icon = Icons.Default.LocationOn,
        benefits = listOf(
            "Accurate distance and pace tracking",
            "Real-time route mapping",
            "Elevation and terrain analysis",
            "Safety features and location sharing"
        ),
        buttonText = "Grant Location Permission",
        onButtonClick = onContinue,
        modifier = modifier
    )
}

@Composable
private fun BackgroundPermissionStep(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionStepCard(
        title = "Allow Background Location",
        description = "Enable background location to track your runs even when the screen is off or you're using other apps.",
        icon = Icons.Default.Place,
        benefits = listOf(
            "Continuous GPS tracking during runs",
            "Works with music and other apps",
            "Battery-optimized tracking",
            "Never miss a workout milestone"
        ),
        buttonText = "Enable Background Tracking",
        onButtonClick = onContinue,
        modifier = modifier
    )
}

@Composable
private fun BatteryOptimizationStep(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionStepCard(
        title = "Optimize Battery Settings",
        description = "For the best tracking experience, disable battery optimization for FITFO AI.",
        icon = Icons.Default.Battery90,
        benefits = listOf(
            "Reliable GPS tracking",
            "Consistent voice coaching",
            "Uninterrupted workout sessions",
            "Accurate workout data"
        ),
        buttonText = "Continue to App",
        onButtonClick = onContinue,
        modifier = modifier,
        showBatteryNote = true
    )
}

@Composable
private fun PermissionStepCard(
    title: String,
    description: String,
    icon: ImageVector,
    benefits: List<String>,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBatteryNote: Boolean = false
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Benefits
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
            
            if (showBatteryNote) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.Warning.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AppColors.Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You can adjust battery settings later in Android Settings > Apps > FITFO AI > Battery",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.DeepBlue.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action button
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
        }
    }
}