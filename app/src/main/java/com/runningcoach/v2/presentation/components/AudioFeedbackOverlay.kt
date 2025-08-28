package com.runningcoach.v2.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.runningcoach.v2.presentation.theme.AppColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class CoachingMessage(
    val id: String,
    val message: String,
    val timestamp: LocalDateTime,
    val coachName: String,
    val type: CoachingMessageType,
    val isAudioDucked: Boolean = false
)

enum class CoachingMessageType(val displayName: String) {
    PACE_FEEDBACK("Pace Update"),
    MOTIVATION("Motivation"),
    MILESTONE("Milestone"),
    WARNING("Alert"),
    ENCOURAGEMENT("Encouragement"),
    TECHNIQUE("Form Tip")
}

@Composable
fun AudioFeedbackOverlay(
    isVoiceActive: Boolean,
    currentMessage: CoachingMessage?,
    recentMessages: List<CoachingMessage>,
    isAudioDucked: Boolean,
    coachName: String,
    onDismissMessage: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Main overlay for current message
    AnimatedVisibility(
        visible = isVoiceActive && currentMessage != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        currentMessage?.let { message ->
            CurrentCoachingMessageCard(
                message = message,
                isAudioDucked = isAudioDucked,
                onDismiss = onDismissMessage,
                onShowHistory = onShowHistory
            )
        }
    }
}

@Composable
fun AudioFeedbackHistoryDialog(
    messages: List<CoachingMessage>,
    onDismiss: () -> Unit,
    isVisible: Boolean = true
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            CoachingHistoryContent(
                messages = messages,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun CurrentCoachingMessageCard(
    message: CoachingMessage,
    isAudioDucked: Boolean,
    onDismiss: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Auto-dismiss after delay
    LaunchedEffect(message.id) {
        delay(4000) // Show for 4 seconds
        onDismiss()
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with coach info and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Voice active indicator
                    VoiceSpeakingIndicator(
                        isActive = true,
                        coachColor = getCoachColor(message.coachName)
                    )
                    
                    Column {
                        Text(
                            text = message.coachName,
                            style = MaterialTheme.typography.labelMedium,
                            color = getCoachColor(message.coachName),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Audio ducked indicator
                    if (isAudioDucked) {
                        AudioDuckedIndicator()
                    }
                    
                    // History button
                    IconButton(
                        onClick = onShowHistory,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "View History",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Dismiss button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Message content
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    expandVertically() + fadeIn() togetherWith
                    shrinkVertically() + fadeOut()
                },
                label = "messageExpansion"
            ) { expanded ->
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )
            }
            
            if (message.message.length > 100) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isExpanded) "Tap to collapse" else "Tap to expand",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.CoralAccent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CoachingHistoryContent(
    messages: List<CoachingMessage>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DeepBlue.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Coaching History",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${messages.size} messages this session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Message type filter chips (optional enhancement)
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(CoachingMessageType.values()) { type ->
                    val count = messages.count { it.type == type }
                    if (count > 0) {
                        FilterChip(
                            onClick = { /* Filter implementation */ },
                            label = {
                                Text(
                                    text = "${type.displayName} ($count)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = false,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AppColors.Surface.copy(alpha = 0.3f),
                                labelColor = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
            
            // Messages list
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = messages.sortedByDescending { it.timestamp },
                    key = { it.id }
                ) { message ->
                    HistoryMessageCard(message = message)
                }
                
                if (messages.isEmpty()) {
                    item {
                        EmptyHistoryPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMessageCard(
    message: CoachingMessage,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header with coach and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(getCoachColor(message.coachName)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message.coachName.first().toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column {
                        Text(
                            text = message.coachName,
                            style = MaterialTheme.typography.labelMedium,
                            color = getCoachColor(message.coachName),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = message.timestamp.format(timeFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (message.isAudioDucked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Audio Ducked",
                                tint = AppColors.Warning,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Ducked",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Warning,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message content
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EmptyHistoryPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "No Messages",
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No coaching messages yet",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Start your workout to receive AI coaching guidance",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VoiceSpeakingIndicator(
    isActive: Boolean,
    coachColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )
    
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coachColor.copy(alpha = alpha),
                        coachColor.copy(alpha = alpha * 0.5f)
                    )
                )
            )
            .graphicsLayer {
                scaleX = if (isActive) scale else 1f
                scaleY = if (isActive) scale else 1f
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Voice Active",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AudioDuckedIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Warning.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Audio Ducked",
                tint = AppColors.Warning,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Music Lowered",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Warning,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getCoachColor(coachName: String): Color {
    return when (coachName.lowercase()) {
        "bennett" -> AppColors.AthleteBlue
        "mariana" -> AppColors.CoralAccent
        "becs" -> AppColors.Success
        "goggins" -> AppColors.Error
        else -> AppColors.CoralAccent
    }
}