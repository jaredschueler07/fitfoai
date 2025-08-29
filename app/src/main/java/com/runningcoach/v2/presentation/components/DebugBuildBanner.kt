package com.runningcoach.v2.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.runningcoach.v2.BuildConfig

@Composable
fun DebugBuildBanner(modifier: Modifier = Modifier) {
    if (!BuildConfig.DEBUG) return

    val line1 = "${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})"
    val aiProvider = BuildConfig.AI_PROVIDER.ifBlank { "GEMINI" }
    val sha = BuildConfig.GIT_SHA.take(7)
    val line2 = "AI: $aiProvider • $sha"

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
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
    }
}

