package com.runningcoach.v2.presentation.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runningcoach.v2.data.local.FITFOAIDatabase
import com.runningcoach.v2.data.repository.GoogleFitRepository
import com.runningcoach.v2.data.repository.UserRepository
import com.runningcoach.v2.domain.model.SampleCoaches
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.CompactButton
import com.runningcoach.v2.presentation.components.ErrorSnackbar
import com.runningcoach.v2.presentation.components.WorkoutCard
import com.runningcoach.v2.presentation.components.icons.PlusIcon
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun DashboardScreen(
    onStartRun: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { FITFOAIDatabase.getDatabase(context) }
    val userRepository = remember { UserRepository(database) }
    val googleFitRepository = remember { GoogleFitRepository(context, database) }
    
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(userRepository, googleFitRepository)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Header with Welcome and Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.Neutral400
                    )
                    if (uiState.isLoadingUser) {
                        CircularProgressIndicator(
                            color = AppColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = uiState.userName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnBackground
                        )
                    }
                }
                
                // Profile Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AppColors.Neutral800),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.userName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.OnSurface
                    )
                }
            }
        }

        item {
            // Today's Guided Run Card
            AppCard {
                Column {
                    Text(
                        text = "Today's Guided Run",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    
                    Text(
                        text = "with ${SampleCoaches.bennett.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.Neutral400,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "30:00",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary
                            )
                            Text(
                                text = uiState.todaysWorkout.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.Neutral500
                            )
                        }
                        
                        CompactButton(
                            text = "Start",
                            onClick = onStartRun,
                            backgroundColor = AppColors.Primary,
                            contentColor = AppColors.OnPrimary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlusIcon(
                                    modifier = Modifier.size(16.dp),
                                    tint = AppColors.OnPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start")
                            }
                        }
                    }
                }
            }
        }

        item {
            // Weekly Activity Chart Placeholder
            Column {
                Text(
                    text = "Weekly Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                AppCard {
                    // Simple chart placeholder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
                        val activities = uiState.weeklyActivity // Minutes
                        
                        weekDays.forEachIndexed { index, day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Simple bar chart representation
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height((activities[index] * 2).dp.coerceAtLeast(4.dp))
                                        .background(
                                            if (activities[index] > 0) AppColors.Primary else AppColors.Neutral700,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                                        )
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.Neutral400
                                )
                            }
                        }
                    }
                }
            }
        }

        // Google Fit Data Section
        if (uiState.fitnessData != null) {
            item {
                Column {
                    Text(
                        text = "Today's Fitness Data",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    AppCard {
                        if (uiState.isLoadingFitnessData) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AppColors.Primary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else if (uiState.fitnessData != null) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Steps
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Steps",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AppColors.OnSurface
                                    )
                                    Text(
                                        text = "${uiState.fitnessData.steps ?: 0}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                    )
                                }
                                
                                // Distance
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Distance",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AppColors.OnSurface
                                    )
                                    Text(
                                        text = "${String.format("%.1f", (uiState.fitnessData.distance ?: 0f) * 0.000621371f)} mi",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                    )
                                }
                                
                                // Calories
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Calories",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AppColors.OnSurface
                                    )
                                    Text(
                                        text = "${uiState.fitnessData.calories ?: 0}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                    )
                                }
                                
                                // Heart Rate (if available)
                                uiState.fitnessData.averageHeartRate?.let { heartRate ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Heart Rate",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = AppColors.OnSurface
                                        )
                                        Text(
                                            text = "${String.format("%.0f", heartRate)} BPM",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.Primary
                                        )
                                    }
                                }
                                
                                // Weight (if available)
                                uiState.fitnessData.weight?.let { weight ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Weight",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = AppColors.OnSurface
                                        )
                                        Text(
                                            text = "${String.format("%.1f", weight * 2.20462f)} lbs",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.Primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No fitness data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.Neutral400,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            // Training Plan Section
            Column {
                Text(
                    text = "Your Plan: ${uiState.trainingPlanName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.upcomingWorkouts.forEach { workout ->
                        WorkoutCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = workout.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppColors.OnSurface
                                )
                                
                                Text(
                                    text = "${workout.duration} min",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.Neutral500
                                )
                            }
                        }
                    }
                    
                    // Rest day
                    WorkoutCard(
                        backgroundColor = AppColors.Neutral800.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Week 1, Day 4: Rest Day",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.Neutral400
                        )
                    }
                }
            }
        }

        item {
            // Past Workouts Section
            Column {
                Text(
                    text = "Past Workouts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.pastWorkouts.forEach { workout ->
                        WorkoutCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = workout.date,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.OnSurface
                                    )
                                    Text(
                                        text = workout.type,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.Neutral400
                                    )
                                }
                                
                                Text(
                                    text = workout.duration,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.OnSurface,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Bottom padding for navigation bar
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    
    // Error Snackbar
    ErrorSnackbar(
        message = uiState.errorMessage ?: "",
        isVisible = uiState.errorMessage != null,
        onDismiss = { viewModel.clearError() },
        actionLabel = "Retry",
        onActionClick = { viewModel.retryDataLoad() }
    )
}
