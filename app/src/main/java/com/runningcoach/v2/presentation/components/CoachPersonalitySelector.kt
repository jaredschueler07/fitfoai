package com.runningcoach.v2.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.presentation.theme.AppColors

data class CoachPersonalityProfile(
    val id: String,
    val name: String,
    val description: String,
    val personalityTraits: List<String>,
    val coachingStyle: String,
    val motivationLevel: String,
    val sampleMessage: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
fun CoachPersonalitySelector(
    selectedCoachId: String?,
    onCoachSelected: (CoachPersonalityProfile) -> Unit,
    onPreviewVoice: (CoachPersonalityProfile) -> Unit,
    isPreviewPlaying: Boolean = false,
    previewingCoachId: String? = null,
    modifier: Modifier = Modifier
) {
    val coaches = remember {
        listOf(
            CoachPersonalityProfile(
                id = "bennett",
                name = "Bennett",
                description = "Professional & Encouraging",
                personalityTraits = listOf("Data-driven", "Supportive", "Technical", "Consistent"),
                coachingStyle = "Combines performance metrics with positive reinforcement",
                motivationLevel = "Steady & Professional",
                sampleMessage = "Great pace! Your heart rate is in the optimal zone. Keep this rhythm for maximum efficiency.",
                color = AppColors.AthleteBlue,
                icon = Icons.Default.TrendingUp
            ),
            CoachPersonalityProfile(
                id = "mariana",
                name = "Mariana",
                description = "Energetic & Upbeat",
                personalityTraits = listOf("Enthusiastic", "Celebratory", "High-energy", "Inspiring"),
                coachingStyle = "High-energy motivation with celebration of achievements",
                motivationLevel = "High Energy",
                sampleMessage = "¡Increíble! You're crushing this run! Your energy is contagious - keep that fire burning!",
                color = AppColors.CoralAccent,
                icon = Icons.Default.Flash
            ),
            CoachPersonalityProfile(
                id = "becs",
                name = "Becs",
                description = "Calm & Mindful",
                personalityTraits = listOf("Zen-like", "Supportive", "Mindful", "Gentle"),
                coachingStyle = "Mindful guidance with focus on form and breathing",
                motivationLevel = "Calm & Steady",
                sampleMessage = "Feel your breath, feel your stride. You're exactly where you need to be. Trust the process.",
                color = AppColors.Success,
                icon = Icons.Default.SelfImprovement
            ),
            CoachPersonalityProfile(
                id = "goggins",
                name = "Goggins",
                description = "Intense & No-Excuses",
                personalityTraits = listOf("Intense", "Challenging", "Mental toughness", "No-excuses"),
                coachingStyle = "Mental toughness training with challenging push beyond comfort zone",
                motivationLevel = "Maximum Intensity",
                sampleMessage = "This is where champions are made! When your mind says stop, you keep going! Stay hard!",
                color = AppColors.Error,
                icon = Icons.Default.LocalFireDepartment
            )
        )
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        text = "Choose Your Coach",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select the coaching personality that motivates you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Voice Coaching",
                    tint = AppColors.CoralAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Coach Grid
            coaches.chunked(2).forEach { rowCoaches ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowCoaches.forEach { coach ->
                        CoachPersonalityCard(
                            coach = coach,
                            isSelected = selectedCoachId == coach.id,
                            isPreviewPlaying = isPreviewPlaying && previewingCoachId == coach.id,
                            onSelected = { onCoachSelected(coach) },
                            onPreview = { onPreviewVoice(coach) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Fill remaining space if odd number
                    if (rowCoaches.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                
                if (rowCoaches != coaches.chunked(2).last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // Selected coach details
            selectedCoachId?.let { coachId ->
                val selectedCoach = coaches.find { it.id == coachId }
                selectedCoach?.let { coach ->
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    SelectedCoachDetails(
                        coach = coach,
                        onPreview = { onPreviewVoice(coach) },
                        isPreviewPlaying = isPreviewPlaying && previewingCoachId == coach.id
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachPersonalityCard(
    coach: CoachPersonalityProfile,
    isSelected: Boolean,
    isPreviewPlaying: Boolean,
    onSelected: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor by animateColorAsState(
        targetValue = if (isSelected) {
            coach.color.copy(alpha = 0.2f)
        } else {
            AppColors.Neutral800.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "cardColor"
    )
    
    Card(
        modifier = modifier
            .clickable { onSelected() }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(coach.color, coach.color.copy(alpha = 0.7f))
                )
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Coach Avatar with animation
            Box(
                modifier = Modifier
                    .size(if (isSelected) 64.dp else 56.dp)
                    .clip(RoundedCornerShape(if (isSelected) 32.dp else 28.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                coach.color,
                                coach.color.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .animateContentSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isPreviewPlaying) {
                    VoiceWaveAnimation(color = Color.White)
                } else {
                    Icon(
                        imageVector = coach.icon,
                        contentDescription = coach.name,
                        tint = Color.White,
                        modifier = Modifier.size(if (isSelected) 32.dp else 28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Coach Name
            Text(
                text = coach.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) coach.color else Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // Description
            Text(
                text = coach.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Motivation Level Badge
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = coach.color.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = coach.motivationLevel,
                    style = MaterialTheme.typography.labelSmall,
                    color = coach.color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Preview Button
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isSelected) coach.color else Color.White.copy(alpha = 0.8f)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(
                        colors = if (isSelected) {
                            listOf(coach.color, coach.color.copy(alpha = 0.6f))
                        } else {
                            listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))
                        }
                    )
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Stop Preview" else "Preview Voice",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPreviewPlaying) "Stop" else "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedCoachDetails(
    coach: CoachPersonalityProfile,
    onPreview: () -> Unit,
    isPreviewPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = coach.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${coach.name}'s Coaching Style",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = onPreview,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = coach.color
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPreviewPlaying) "Stop" else "Try Voice",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Coaching Style
            Text(
                text = "Coaching Approach:",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = coach.coachingStyle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Personality Traits
            Text(
                text = "Personality Traits:",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(coach.personalityTraits) { trait ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = coach.color.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = trait,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sample Message
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.Neutral800.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Quote",
                            tint = coach.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sample Coaching:",
                            style = MaterialTheme.typography.labelMedium,
                            color = coach.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = coach.sampleMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveAnimation(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceWave")
    val animatedValues = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600,
                    delayMillis = index * 100,
                    easing = EaseInOut
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave$index"
        )
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        animatedValues.forEach { animatedValue ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * animatedValue.value).dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}