package com.runningcoach.v2.presentation.screen.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Header
            Column(
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnBackground
                )
                
                Text(
                    text = "Track your fitness journey and achievements",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Neutral400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        item {
            // Weekly Summary
            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "This Week",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProgressItem(
                            value = "7.8",
                            unit = "mi",
                            label = "Distance"
                        )
                        ProgressItem(
                            value = "3",
                            unit = "runs",
                            label = "Sessions"
                        )
                        ProgressItem(
                            value = "2:15",
                            unit = "hrs",
                            label = "Time"
                        )
                    }
                }
            }
        }
        
        item {
            // Monthly Goals
            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Monthly Goals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    GoalProgressItem(
                        title = "Distance Goal",
                        current = 28.1f,
                        target = 37.3f,
                        unit = "mi"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    GoalProgressItem(
                        title = "Run Sessions",
                        current = 8f,
                        target = 12f,
                        unit = "runs"
                    )
                }
            }
        }
        
        item {
            // Recent Achievements
            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Recent Achievements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AchievementItem(
                        title = "Personal Best 5K",
                        description = "New record: 24:32",
                        date = "2 days ago"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AchievementItem(
                        title = "Consistency Streak",
                        description = "7 days running streak",
                        date = "Today"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AchievementItem(
                        title = "Distance Milestone",
                        description = "Reached 62 miles total",
                        date = "1 week ago"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressItem(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.Primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Neutral500
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.OnSurface
        )
    }
}

@Composable
private fun GoalProgressItem(
    title: String,
    current: Float,
    target: Float,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.OnSurface
            )
            Text(
                text = "${current.toInt()}/${target.toInt()} $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Neutral400
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = (current / target).coerceAtMost(1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = AppColors.Primary,
            trackColor = AppColors.Neutral700
        )
    }
}

@Composable
private fun AchievementItem(
    title: String,
    description: String,
    date: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    AppColors.Primary.copy(alpha = 0.1f),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🏆",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.OnSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Neutral400
            )
        }
        
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Neutral500
        )
    }
}
