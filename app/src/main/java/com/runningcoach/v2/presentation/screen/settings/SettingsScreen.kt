package com.runningcoach.v2.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.presentation.theme.AppColors
import com.runningcoach.v2.presentation.components.VoiceCoachingCard
import com.runningcoach.v2.presentation.components.CoachPersonality
import com.runningcoach.v2.presentation.components.CoachingFrequency
import com.runningcoach.v2.presentation.components.VoiceStatusIndicator
import com.runningcoach.v2.presentation.components.VoiceStatusData
import com.runningcoach.v2.presentation.components.VoiceStatus

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Settings state management
    var selectedCoach by remember { 
        mutableStateOf<CoachPersonality?>(
            CoachPersonality(
                id = "bennett",
                name = "Bennett", 
                description = "Professional & encouraging",
                style = "Data-driven coaching with positive reinforcement"
            )
        )
    }
    var coachingEnabled by remember { mutableStateOf(true) }
    var voiceVolume by remember { mutableStateOf(0.8f) }
    var coachingFrequency by remember { mutableStateOf(CoachingFrequency.MEDIUM) }
    var isVoiceActive by remember { mutableStateOf(false) }
    
    // Voice status
    var voiceStatus by remember { 
        mutableStateOf(
            VoiceStatusData(
                status = VoiceStatus.INACTIVE,
                isCoachingEnabled = coachingEnabled,
                currentCoach = selectedCoach?.name
            )
        )
    }
    
    // Notification settings
    var notificationsEnabled by remember { mutableStateOf(true) }
    var workoutReminders by remember { mutableStateOf(true) }
    var achievementAlerts by remember { mutableStateOf(true) }
    
    // Privacy settings
    var dataSharing by remember { mutableStateOf(false) }
    var analyticsEnabled by remember { mutableStateOf(true) }
    
    // Performance settings
    var backgroundLocation by remember { mutableStateOf(true) }
    var batteryOptimization by remember { mutableStateOf(false) }
    var highAccuracyGPS by remember { mutableStateOf(true) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.GradientStart,
                        AppColors.GradientEnd
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize your FITFO AI experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            item {
                // Voice Coaching Section
                VoiceCoachingCard(
                    selectedCoach = selectedCoach,
                    coachingEnabled = coachingEnabled,
                    volume = voiceVolume,
                    coachingFrequency = coachingFrequency,
                    isVoiceActive = isVoiceActive,
                    onCoachSelected = { coach ->
                        selectedCoach = coach
                        voiceStatus = voiceStatus.copy(currentCoach = coach.name)
                    },
                    onCoachingToggle = { enabled ->
                        coachingEnabled = enabled
                        voiceStatus = voiceStatus.copy(
                            status = if (enabled) VoiceStatus.INACTIVE else VoiceStatus.INACTIVE,
                            isCoachingEnabled = enabled
                        )
                    },
                    onVolumeChange = { volume -> voiceVolume = volume },
                    onFrequencyChange = { frequency -> coachingFrequency = frequency },
                    onPreviewVoice = { coach ->
                        isVoiceActive = true
                        voiceStatus = voiceStatus.copy(status = VoiceStatus.SPEAKING)
                        // Simulate voice playback ending
                        // In real app, this would be managed by VoiceCoachingManager
                    }
                )
            }
            
            item {
                // Voice Status Indicator
                VoiceStatusIndicator(
                    statusData = voiceStatus,
                    onClick = { /* Show detailed voice status */ }
                )
            }
            
            item {
                // Coaching Triggers Section
                SettingsSection(
                    title = "Coaching Triggers",
                    description = "When should your coach provide guidance?",
                    content = {
                        CoachingTriggersSettings()
                    }
                )
            }
            
            item {
                // Audio & Sound Section
                SettingsSection(
                    title = "Audio & Sound",
                    description = "Manage audio output and ducking preferences",
                    content = {
                        AudioSettings(
                            voiceVolume = voiceVolume,
                            onVolumeChange = { voiceVolume = it }
                        )
                    }
                )
            }
            
            item {
                // Notifications Section
                SettingsSection(
                    title = "Notifications",
                    description = "Manage your notification preferences",
                    content = {
                        NotificationSettings(
                            notificationsEnabled = notificationsEnabled,
                            workoutReminders = workoutReminders,
                            achievementAlerts = achievementAlerts,
                            onNotificationsToggle = { notificationsEnabled = it },
                            onWorkoutRemindersToggle = { workoutReminders = it },
                            onAchievementAlertsToggle = { achievementAlerts = it }
                        )
                    }
                )
            }
            
            item {
                // Performance Section
                SettingsSection(
                    title = "Performance & GPS",
                    description = "Optimize tracking accuracy and battery usage",
                    content = {
                        PerformanceSettings(
                            backgroundLocation = backgroundLocation,
                            batteryOptimization = batteryOptimization,
                            highAccuracyGPS = highAccuracyGPS,
                            onBackgroundLocationToggle = { backgroundLocation = it },
                            onBatteryOptimizationToggle = { batteryOptimization = it },
                            onHighAccuracyGPSToggle = { highAccuracyGPS = it }
                        )
                    }
                )
            }
            
            item {
                // Privacy & Data Section
                SettingsSection(
                    title = "Privacy & Data",
                    description = "Control your data sharing and privacy settings",
                    content = {
                        PrivacySettings(
                            dataSharing = dataSharing,
                            analyticsEnabled = analyticsEnabled,
                            onDataSharingToggle = { dataSharing = it },
                            onAnalyticsToggle = { analyticsEnabled = it }
                        )
                    }
                )
            }
            
            item {
                // App Info Section
                SettingsSection(
                    title = "App Information",
                    description = "Version, support, and legal information",
                    content = {
                        AppInfoSettings()
                    }
                )
            }
            
            // Bottom spacing for navigation
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            
            content()
        }
    }
}

@Composable
private fun CoachingTriggersSettings() {
    val triggers = listOf(
        "Pace changes" to true,
        "Distance milestones" to true,
        "Heart rate zones" to false,
        "Form corrections" to true,
        "Motivation boosts" to true,
        "Interval training" to false
    )
    
    triggers.forEach { (trigger, enabled) ->
        var isEnabled by remember { mutableStateOf(enabled) }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trigger,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.CoralAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = AppColors.Neutral700
                )
            )
        }
    }
}

@Composable
private fun AudioSettings(
    voiceVolume: Float,
    onVolumeChange: (Float) -> Unit
) {
    var musicDucking by remember { mutableStateOf(true) }
    var systemVolume by remember { mutableStateOf(0.7f) }
    
    Column {
        // Voice vs Music Balance
        Text(
            text = "Voice Volume",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Adjust coach voice volume relative to music",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VolumeDown,
                contentDescription = "Lower Volume",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            
            Slider(
                value = voiceVolume,
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
                contentDescription = "Raise Volume",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Music Ducking
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Music Ducking",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Lower music volume when coach speaks",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = musicDucking,
                onCheckedChange = { musicDucking = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.CoralAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = AppColors.Neutral700
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Audio Output Device
        SettingItem(
            title = "Audio Output",
            subtitle = "Bluetooth Headphones",
            icon = Icons.Default.Headset,
            onClick = { /* Show audio output options */ }
        )
    }
}

@Composable
private fun NotificationSettings(
    notificationsEnabled: Boolean,
    workoutReminders: Boolean,
    achievementAlerts: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    onWorkoutRemindersToggle: (Boolean) -> Unit,
    onAchievementAlertsToggle: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Receive workout reminders and achievement alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.CoralAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = AppColors.Neutral700
                )
            )
        }
        
        if (notificationsEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workout Reminders",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Switch(
                        checked = workoutReminders,
                        onCheckedChange = onWorkoutRemindersToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppColors.CoralAccent,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = AppColors.Neutral700
                        )
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Achievement Alerts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Switch(
                        checked = achievementAlerts,
                        onCheckedChange = onAchievementAlertsToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppColors.CoralAccent,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = AppColors.Neutral700
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceSettings(
    backgroundLocation: Boolean,
    batteryOptimization: Boolean,
    highAccuracyGPS: Boolean,
    onBackgroundLocationToggle: (Boolean) -> Unit,
    onBatteryOptimizationToggle: (Boolean) -> Unit,
    onHighAccuracyGPSToggle: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Background Location",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Track runs when app is in background",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = backgroundLocation,
                onCheckedChange = onBackgroundLocationToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.CoralAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = AppColors.Neutral700
                )
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "High Accuracy GPS",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Use more battery for better accuracy",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = highAccuracyGPS,
                onCheckedChange = onHighAccuracyGPSToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.CoralAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = AppColors.Neutral700
                )
            )
        }
        
        if (!batteryOptimization) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.Warning.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Battery Warning",
                        tint = AppColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Battery optimization detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = { /* Open battery settings */ },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = AppColors.Warning
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Optimize Settings",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySettings(
    dataSharing: Boolean,
    analyticsEnabled: Boolean,
    onDataSharingToggle: (Boolean) -> Unit,
    onAnalyticsToggle: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Anonymous Analytics",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Help improve the app with usage data",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = analyticsEnabled,
                onCheckedChange = onAnalyticsToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.CoralAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = AppColors.Neutral700
                )
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Data Sharing",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "Share workout data with connected apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = dataSharing,
                onCheckedChange = onDataSharingToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.CoralAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = AppColors.Neutral700
                )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SettingItem(
            title = "Privacy Policy",
            subtitle = "View our privacy policy",
            icon = Icons.Default.Security,
            onClick = { /* Open privacy policy */ }
        )
        
        SettingItem(
            title = "Data Export",
            subtitle = "Export your workout data",
            icon = Icons.Default.Download,
            onClick = { /* Open data export */ }
        )
    }
}

@Composable
private fun AppInfoSettings() {
    Column {
        SettingItem(
            title = "Version",
            subtitle = "1.0.0 (Beta)",
            icon = Icons.Default.Info,
            onClick = null
        )
        
        SettingItem(
            title = "Help & Support",
            subtitle = "Get help and contact support",
            icon = Icons.Default.HelpOutline,
            onClick = { /* Open support */ }
        )
        
        SettingItem(
            title = "Terms of Service",
            subtitle = "View terms and conditions",
            icon = Icons.Default.Article,
            onClick = { /* Open terms */ }
        )
        
        SettingItem(
            title = "Rate the App",
            subtitle = "Leave a review in the app store",
            icon = Icons.Default.Star,
            onClick = { /* Open app store rating */ }
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AppColors.CoralAccent,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Navigate",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}