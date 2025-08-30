package com.runningcoach.v2.presentation.screen.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.presentation.components.AppCard
import com.runningcoach.v2.presentation.components.CompactSourceBadge
import com.runningcoach.v2.presentation.components.AppDataSource
import com.runningcoach.v2.presentation.components.BadgeVariant
import com.runningcoach.v2.presentation.theme.AppColors
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import com.runningcoach.v2.RunningCoachApplication

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel
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
            val stats = viewModel.weeklyStats.collectAsState().value
            AppCard {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "This Week (by Source)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CompactSourceBadge(source = AppDataSource.FITFOAI)
                                Spacer(Modifier.width(6.dp))
                                Text("${String.format("%.1f", stats.fitfoMiles)} mi", color = AppColors.Primary, fontWeight = FontWeight.Bold)
                            }
                            Text("${stats.fitfoRuns} runs", color = AppColors.Neutral500)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CompactSourceBadge(source = AppDataSource.GOOGLE_FIT)
                                Spacer(Modifier.width(6.dp))
                                Text("${String.format("%.1f", stats.fitMiles)} mi", color = AppColors.Primary, fontWeight = FontWeight.Bold)
                            }
                            Text("${stats.fitRuns} runs", color = AppColors.Neutral500)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${String.format("%.1f", stats.totalMiles)} mi", color = AppColors.OnSurface, fontWeight = FontWeight.Bold)
                            Text("${stats.totalRuns} total runs", color = AppColors.Neutral500)
                        }
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
            // Run History (latest 20)
            AppCard {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "Recent Runs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    val items = viewModel.runItems.collectAsState().value
                    if (items.isEmpty()) {
                        Text("No runs yet", color = AppColors.Neutral400)
                    } else {
                        items.forEach { run ->
                            RunHistoryRow(run)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
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
private fun RunHistoryRow(item: RunListItem) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(item.date, style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral500)
            Text(item.distanceMiles, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.OnSurface)
            Text(item.pacePerMile, style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CompactSourceBadge(
            source = when (item.source) {
                com.runningcoach.v2.domain.repository.SessionSource.GOOGLE_FIT -> AppDataSource.GOOGLE_FIT
                else -> AppDataSource.FITFOAI
            }
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
