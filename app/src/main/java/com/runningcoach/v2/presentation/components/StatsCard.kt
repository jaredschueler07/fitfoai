package com.runningcoach.v2.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Metric type enumeration for different statistics
 */
enum class MetricType {
    DISTANCE,
    PACE,
    TIME,
    RUNS,
    CALORIES,
    HEART_RATE
}

/**
 * Time period enumeration for statistics filtering
 */
enum class TimePeriod(val label: String, val days: Int) {
    SEVEN_DAYS("7 Days", 7),
    THIRTY_DAYS("30 Days", 30),
    ALL_TIME("All Time", Int.MAX_VALUE)
}

/**
 * Data class representing a statistic with trend information
 */
data class StatisticData(
    val value: Double,
    val unit: String,
    val trend: Double? = null, // Percentage change, null if no trend
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Statistics card component for displaying run metrics with trends and period selection
 * Following the athletic blue design system with Material 3 guidelines
 *
 * @param title The title of the statistic
 * @param metricType The type of metric being displayed
 * @param data The statistic data including value, unit, and trend
 * @param selectedPeriod Currently selected time period
 * @param onPeriodChange Callback when period is changed
 * @param showSourceFilter Whether to show source filter toggle
 * @param onSourceFilterToggle Callback for source filter changes
 * @param modifier Modifier for the card
 */
@Composable
fun StatsCard(
    title: String,
    metricType: MetricType,
    data: StatisticData,
    selectedPeriod: TimePeriod = TimePeriod.SEVEN_DAYS,
    onPeriodChange: (TimePeriod) -> Unit = {},
    showSourceFilter: Boolean = false,
    onSourceFilterToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var animatedValue by remember { mutableStateOf(0.0) }
    
    // Animate value changes
    LaunchedEffect(data.value) {
        if (!data.isLoading) {
            val startValue = animatedValue
            val targetValue = data.value
            val steps = 30
            val stepValue = (targetValue - startValue) / steps
            
            repeat(steps) { step ->
                animatedValue = startValue + (stepValue * step)
                delay(16) // ~60fps
            }
            animatedValue = targetValue
        }
    }

    AppCard(
        modifier = modifier,
        backgroundColor = AppColors.CardBackground,
        borderColor = AppColors.CardBorder
    ) {
        Column {
            // Header with title and period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface
                )
                
                // Period Selector
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showPeriodDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedPeriod.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.Primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select period",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showPeriodDropdown,
                        onDismissRequest = { showPeriodDropdown = false }
                    ) {
                        TimePeriod.values().forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.label) },
                                onClick = {
                                    onPeriodChange(period)
                                    showPeriodDropdown = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main statistic display
            when {
                data.isLoading -> {
                    LoadingState()
                }
                data.error != null -> {
                    ErrorState(data.error)
                }
                else -> {
                    StatisticDisplay(
                        value = if (data.isLoading) data.value else animatedValue,
                        unit = data.unit,
                        trend = data.trend,
                        metricType = metricType
                    )
                }
            }
            
            // Source filter toggle (if enabled)
            if (showSourceFilter) {
                Spacer(modifier = Modifier.height(12.dp))
                SourceFilterToggle(
                    onToggle = onSourceFilterToggle
                )
            }
        }
    }
}

/**
 * Main statistic display with value, unit, and trend
 */
@Composable
private fun StatisticDisplay(
    value: Double,
    unit: String,
    trend: Double?,
    metricType: MetricType
) {
    Column {
        // Main value with animated transitions
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()).togetherWith(
                    slideOutVertically { -it } + fadeOut()
                )
            },
            label = "value_animation"
        ) { animatedValue ->
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatValue(animatedValue, metricType),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AppColors.OnSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        
        // Trend indicator
        if (trend != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TrendIndicator(trend)
        }
    }
}

/**
 * Trend indicator showing percentage change with color coding and arrow
 */
@Composable
private fun TrendIndicator(trend: Double) {
    val isPositive = trend > 0
    val isNeutral = trend == 0.0
    val color = when {
        isNeutral -> AppColors.OnSurfaceVariant
        isPositive -> AppColors.Success
        else -> AppColors.Error
    }
    val icon = when {
        isNeutral -> null
        isPositive -> Icons.Default.KeyboardArrowUp
        else -> Icons.Default.KeyboardArrowDown
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPositive) AppColors.Success.copy(alpha = 0.1f) 
                else if (isNeutral) Color.Transparent 
                else AppColors.Error.copy(alpha = 0.1f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        icon?.let { iconVector ->
            Icon(
                imageVector = iconVector,
                contentDescription = if (isPositive) "Positive trend" else "Negative trend",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        Text(
            text = "${if (trend > 0) "+" else ""}${DecimalFormat("#.#").format(trend)}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Loading state for when data is being fetched
 */
@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shimmer effect placeholder
        Box(
            modifier = Modifier
                .height(48.dp)
                .width(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            AppColors.CardBorder,
                            AppColors.CardBorder.copy(alpha = 0.5f),
                            AppColors.CardBorder
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.OnSurfaceVariant
        )
    }
}

/**
 * Error state when data fails to load
 */
@Composable
private fun ErrorState(error: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "—",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            ),
            color = AppColors.Error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Error,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Source filter toggle for filtering between FITFOAI and Google Fit data
 */
@Composable
private fun SourceFilterToggle(
    onToggle: (Boolean) -> Unit
) {
    var isFiltered by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(8.dp))
            .clickable {
                isFiltered = !isFiltered
                onToggle(isFiltered)
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filter by source",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.OnSurface
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            SourceBadge(
                source = AppDataSource.FITFOAI,
                variant = BadgeVariant.OUTLINED
            )
            Spacer(modifier = Modifier.width(4.dp))
            SourceBadge(
                source = AppDataSource.GOOGLE_FIT,
                variant = BadgeVariant.OUTLINED
            )
        }
    }
}

/**
 * Formats numeric values based on metric type with proper localization
 */
private fun formatValue(value: Double, metricType: MetricType): String {
    return when (metricType) {
        MetricType.DISTANCE -> {
            when {
                value >= 1.0 -> DecimalFormat("#.##").format(value)
                else -> DecimalFormat("#.###").format(value)
            }
        }
        MetricType.PACE -> {
            val minutes = value.toInt()
            val seconds = ((value - minutes) * 60).toInt()
            String.format("%d:%02d", minutes, seconds)
        }
        MetricType.TIME -> {
            val hours = (value / 60).toInt()
            val minutes = (value % 60).toInt()
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
        MetricType.RUNS, MetricType.CALORIES, MetricType.HEART_RATE -> {
            NumberFormat.getNumberInstance(Locale.US).format(value.toInt())
        }
    }
}