package com.runningcoach.v2.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.runningcoach.v2.BuildConfig

@Composable
fun DebugBuildBanner(
    modifier: Modifier = Modifier,
    showPerformanceMetrics: Boolean = false,
    onToggleMetrics: () -> Unit = {}
) {
    if (!BuildConfig.DEBUG) return

    val line1 = "${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})"
    val aiProvider = BuildConfig.AI_PROVIDER.ifBlank { "GEMINI" }
    val sha = BuildConfig.GIT_SHA.take(7)
    val line2 = "AI: $aiProvider • $sha"

    // Try to get performance metrics if available
    val performanceMetrics = remember {
        runCatching {
            val performanceConfigClass = Class.forName("com.runningcoach.v2.config.PerformanceConfig")
            val metricsField = performanceConfigClass.getDeclaredField("performanceMetrics")
            metricsField.isAccessible = true
            metricsField.get(null) as kotlinx.coroutines.flow.StateFlow<*>
        }.getOrNull()
    }
    
    val metricsValue = performanceMetrics?.collectAsState()?.value

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEBUG",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = line1,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = line2,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                
                if (metricsValue != null) {
                    TextButton(
                        onClick = onToggleMetrics,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = if (showPerformanceMetrics) "Hide" else "DB",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            if (showPerformanceMetrics && metricsValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                DatabasePerformanceMetrics(metrics = metricsValue)
            }
        }
    }
}

@Composable
private fun DatabasePerformanceMetrics(metrics: Any) {
    val metricsData = remember(metrics) {
        runCatching {
            val metricsClass = metrics.javaClass
            Triple(
                metricsClass.getDeclaredField("totalQueries").getLong(metrics),
                metricsClass.getDeclaredField("slowQueries").getLong(metrics),
                metricsClass.getDeclaredField("slowestQueryTime").getLong(metrics)
            )
        }.getOrNull()
    }
    
    if (metricsData != null) {
        val (totalQueries, slowQueries, slowestQueryTime) = metricsData
        
        Column {
            Text(
                text = "DATABASE PERFORMANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Row {
                Text(
                    text = "Queries: $totalQueries",
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Slow: $slowQueries", 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (slowQueries > 0) MaterialTheme.colorScheme.error 
                           else MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            if (slowestQueryTime > 0) {
                Text(
                    text = "Slowest: ${slowestQueryTime}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (slowestQueryTime > 100L) // Using hardcoded threshold since BuildConfig may not have it
                           MaterialTheme.colorScheme.error 
                           else MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    } else {
        Text(
            text = "Performance metrics unavailable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

