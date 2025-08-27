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
import com.runningcoach.v2.domain.model.SampleCoaches
import com.runningcoach.v2.domain.model.SampleTrainingData
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.CompactButton
import com.runningcoach.v2.presentation.components.WorkoutCard
import com.runningcoach.v2.presentation.components.icons.PlusIcon
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun DashboardScreen(
    userName: String = "Runner",
    onStartRun: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { com.runningcoach.v2.data.local.FITFOAIDatabase.getDatabase(context) }
    val googleFitRepository = remember { com.runningcoach.v2.data.repository.GoogleFitRepository(context, database) }
    
    var fitnessData by remember { mutableStateOf<com.runningcoach.v2.data.local.entity.GoogleFitDailySummaryEntity?>(null) }
    var isLoadingFitnessData by remember { mutableStateOf(false) }
    
    // Load fitness data when component is created
    LaunchedEffect(Unit) {
        if (googleFitRepository.isGoogleFitConnected()) {
            isLoadingFitnessData = true
            try {
                // Try to get cached data first
                val cachedData = googleFitRepository.getTodaysFitnessData()
                if (cachedData != null) {
                    fitnessData = cachedData
                }
                
                // Sync fresh data from Google Fit
                val syncResult = googleFitRepository.syncTodaysFitnessData()
                if (syncResult.isSuccess) {
                    fitnessData = syncResult.getOrNull()
                }
            } catch (e: Exception) {
                // Handle error silently for now
            } finally {
                isLoadingFitnessData = false
            }
        }
    }
    
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
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnBackground
                    )
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
                        text = userName.take(1).uppercase(),
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
                                text = SampleTrainingData.todaysWorkout.name,
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
                        val activities = listOf(30, 0, 45, 25, 35, 60, 0) // Minutes
                        
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
        if (fitnessData != null) {
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
                        if (isLoadingFitnessData) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AppColors.Primary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else if (fitnessData != null) {
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
                                        text = "${fitnessData!!.steps ?: 0}",
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
                                        text = "${String.format("%.1f", (fitnessData!!.distance ?: 0f) / 1000)} km",
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
                                        text = "${fitnessData!!.calories ?: 0}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                    )
                                }
                                
                                // Heart Rate (if available)
                                fitnessData!!.averageHeartRate?.let { heartRate ->
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
                                fitnessData!!.weight?.let { weight ->
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
                                            text = "${String.format("%.1f", weight)} kg",
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
                    text = "Your Plan: Marathon Training",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SampleTrainingData.upcomingWorkouts.forEach { workout ->
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
                    SampleTrainingData.pastWorkouts.forEach { workout ->
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
}
