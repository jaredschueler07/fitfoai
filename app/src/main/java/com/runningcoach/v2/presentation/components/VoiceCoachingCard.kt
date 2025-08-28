package com.runningcoach.v2.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.presentation.theme.AppColors

data class CoachPersonality(
    val id: String,
    val name: String,
    val description: String,
    val style: String,
    val color: Color = AppColors.CoralAccent
)

enum class CoachingFrequency(val displayName: String) {
    HIGH("High - Every 30s"),
    MEDIUM("Medium - Every 60s"),
    LOW("Low - Every 2min")
}

@Composable
fun VoiceCoachingCard(
    selectedCoach: CoachPersonality?,
    coachingEnabled: Boolean,
    volume: Float,
    coachingFrequency: CoachingFrequency,
    isVoiceActive: Boolean,
    onCoachSelected: (CoachPersonality) -> Unit,
    onCoachingToggle: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onFrequencyChange: (CoachingFrequency) -> Unit,
    onPreviewVoice: (CoachPersonality) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableCoaches = listOf(
        CoachPersonality(
            id = "bennett",
            name = "Bennett",
            description = "Professional & encouraging",
            style = "Data-driven coaching with positive reinforcement",
            color = AppColors.AthleteBlue
        ),
        CoachPersonality(
            id = "mariana",
            name = "Mariana",
            description = "Energetic & motivational",
            style = "High-energy motivation and celebration",
            color = AppColors.CoralAccent
        ),
        CoachPersonality(
            id = "becs",
            name = "Becs",
            description = "Calm & supportive",
            style = "Mindful guidance with gentle encouragement",
            color = AppColors.Success
        ),
        CoachPersonality(
            id = "goggins",
            name = "Goggins",
            description = "Intense & challenging",
            style = "No-excuses mentality and mental toughness",
            color = AppColors.Error
        )
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Voice Coaching",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AI-powered running guidance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Switch(
                    checked = coachingEnabled,
                    onCheckedChange = onCoachingToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AppColors.CoralAccent,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = AppColors.Neutral700
                    )
                )
            }
            
            AnimatedVisibility(
                visible = coachingEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Selected Coach Display
                    selectedCoach?.let { coach ->
                        SelectedCoachCard(
                            coach = coach,
                            isVoiceActive = isVoiceActive,
                            onPreviewVoice = onPreviewVoice,
                            onChangeCoach = { /* Expand coach selector */ }
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    
                    // Coach Personality Selector
                    if (selectedCoach == null) {
                        Text(
                            text = "Choose Your Coach",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyCoachGrid(
                            coaches = availableCoaches,
                            selectedCoach = selectedCoach,
                            onCoachSelected = onCoachSelected,
                            onPreviewVoice = onPreviewVoice
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    
                    // Volume Control
                    VolumeControlSection(
                        volume = volume,
                        onVolumeChange = onVolumeChange
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Coaching Frequency
                    CoachingFrequencySection(
                        selectedFrequency = coachingFrequency,
                        onFrequencyChange = onFrequencyChange
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedCoachCard(
    coach: CoachPersonality,
    isVoiceActive: Boolean,
    onPreviewVoice: (CoachPersonality) -> Unit,
    onChangeCoach: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = coach.color.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coach Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                coach.color,
                                coach.color.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isVoiceActive) {
                    VoiceActiveIndicator(color = Color.White)
                } else {
                    Text(
                        text = coach.name.first().toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Coach Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coach.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = coach.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = coach.style,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }
            
            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onPreviewVoice(coach) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview Voice",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onChangeCoach,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Change Coach",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyCoachGrid(
    coaches: List<CoachPersonality>,
    selectedCoach: CoachPersonality?,
    onCoachSelected: (CoachPersonality) -> Unit,
    onPreviewVoice: (CoachPersonality) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        coaches.chunked(2).forEach { rowCoaches ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCoaches.forEach { coach ->
                    CoachSelectionCard(
                        coach = coach,
                        isSelected = selectedCoach?.id == coach.id,
                        onSelected = { onCoachSelected(coach) },
                        onPreview = { onPreviewVoice(coach) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Add empty space if odd number of coaches in row
                if (rowCoaches.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CoachSelectionCard(
    coach: CoachPersonality,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onSelected() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                coach.color.copy(alpha = 0.2f)
            } else {
                AppColors.Neutral800.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = coach.color
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Coach Initial/Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(coach.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = coach.name.first().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = coach.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = coach.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Preview button
            IconButton(
                onClick = onPreview,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Preview ${coach.name}",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun VolumeControlSection(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Voice Volume",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.CoralAccent,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VolumeDown,
                contentDescription = "Volume Down",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.CoralAccent,
                    activeTrackColor = AppColors.CoralAccent,
                    inactiveTrackColor = AppColors.Neutral700
                )
            )
            
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Volume Up",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CoachingFrequencySection(
    selectedFrequency: CoachingFrequency,
    onFrequencyChange: (CoachingFrequency) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Coaching Frequency",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoachingFrequency.values().forEach { frequency ->
                FrequencyChip(
                    frequency = frequency,
                    isSelected = frequency == selectedFrequency,
                    onSelected = { onFrequencyChange(frequency) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FrequencyChip(
    frequency: CoachingFrequency,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onSelected() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AppColors.CoralAccent.copy(alpha = 0.2f)
            } else {
                AppColors.Neutral700.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = AppColors.CoralAccent
            )
        } else null
    ) {
        Text(
            text = frequency.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) AppColors.CoralAccent else Color.White.copy(alpha = 0.8f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VoiceActiveIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Voice Active",
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}