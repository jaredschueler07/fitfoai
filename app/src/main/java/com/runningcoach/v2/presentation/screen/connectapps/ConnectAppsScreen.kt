package com.runningcoach.v2.presentation.screen.connectapps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcoach.v2.data.service.*
import com.runningcoach.v2.presentation.theme.AppColors
import com.runningcoach.v2.presentation.theme.AppColors.Companion.Primary
import com.runningcoach.v2.presentation.theme.AppColors.Companion.Neutral400
import com.runningcoach.v2.presentation.theme.AppColors.Companion.Neutral500
import com.runningcoach.v2.presentation.theme.AppColors.Companion.OnSurface
import com.runningcoach.v2.presentation.theme.AppColors.Companion.CardBackground
import com.runningcoach.v2.presentation.theme.AppColors.Companion.CardBorder
import com.runningcoach.v2.presentation.theme.AppColors.Companion.OnBackground
import kotlinx.coroutines.launch

/**
 * Enhanced ConnectAppsScreen with music controls, BPM dashboard, and comprehensive Spotify integration
 * 
 * Features:
 * - Spotify connection status and controls
 * - Real-time BPM matching dashboard
 * - Music playback controls
 * - Playlist management
 * - Audio ducking settings
 * 
 * Reference: https://developer.spotify.com/documentation/android
 */
@Composable
fun ConnectAppsScreen(
    modifier: Modifier = Modifier,
    onComplete: (List<ConnectedApp>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Services
    val spotifyService = remember { SpotifySdkService(context) }
    val bpmAnalysisEngine = remember { BPMAnalysisEngine() }
    val musicCoachingIntegration = remember {
        // Note: MusicCoachingIntegration needs to be updated to work with SpotifySdkService
        // For now, we'll comment it out and focus on basic Spotify functionality
        // MusicCoachingIntegration(context, spotifyService, VoiceCoachingManager(context, null, null, null), bpmAnalysisEngine)
        null
    }
    
    // State
    var connectedApps by remember { mutableStateOf(emptyList<ConnectedApp>()) }
    var connectingApp by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Spotify state
    val spotifyConnected by spotifyService.isConnected.collectAsState()
    val spotifyStatus by spotifyService.connectionStatus.collectAsState()
    val currentTrack by spotifyService.currentTrack.collectAsState()
    val playbackState by spotifyService.playbackState.collectAsState()
    
    // BPM analysis state
    val currentCadence by bpmAnalysisEngine.currentCadence.collectAsState()
    val bpmRecommendation by bpmAnalysisEngine.recommendedBpm.collectAsState()

    // Music coaching state (temporarily disabled until updated for SDK)
    // val musicState by musicCoachingIntegration.musicState.collectAsState()
    // val coachingPriority by musicCoachingIntegration.coachingPriority.collectAsState()
    // val audioDuckingLevel by musicCoachingIntegration.audioDuckingLevel.collectAsState()
    // val musicAwareCoaching by musicCoachingIntegration.musicAwareCoaching.collectAsState()

    // BPM matching suggestion (temporarily disabled)
    // var bpmSuggestion by remember { mutableStateOf<MusicCoachingIntegration.BpmMatchingSuggestion?>(null) }

    // Update BPM suggestion (temporarily disabled)
    // LaunchedEffect(currentCadence, currentTrack) {
    //     bpmSuggestion = musicCoachingIntegration.getBpmMatchingSuggestion()
    // }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Connect Your Apps",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnBackground,
            modifier = Modifier.padding(top = 40.dp, bottom = 8.dp)
        )
        
        Text(
            text = "Connect your fitness and music apps to personalize your training experience",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Neutral400,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Connection status message
        spotifyStatus?.let { status ->
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (spotifyConnected) Primary.copy(alpha = 0.1f) else AppColors.Error.copy(alpha = 0.1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = if (spotifyConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (spotifyConnected) Primary else AppColors.Error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (spotifyConnected) Primary else AppColors.Error
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Spotify Connection Section
            item {
                SpotifyConnectionSection(
                    spotifyConnected = spotifyConnected,
                    connecting = connectingApp == "spotify",
                    onConnect = {
                        connectingApp = "spotify"
                        scope.launch {
                            try {
                                val intent = spotifyService.initiateAuth()
                                // Launch the intent for Spotify authentication
                                context.startActivity(intent)
                                connectingApp = null
                            } catch (e: Exception) {
                                errorMessage = "Failed to connect to Spotify: ${e.message}"
                                connectingApp = null
                            }
                        }
                    },
                    onDisconnect = {
                        spotifyService.disconnect()
                    }
                )
            }
            
            // Music Controls Section (only show if connected)
            if (spotifyConnected) {
                item {
                    MusicControlsSection(
                        currentTrack = currentTrack,
                        playbackState = playbackState,
                        onPlayPause = {
                            scope.launch {
                                spotifyService.togglePlayback()
                            }
                        },
                        onNext = {
                            scope.launch {
                                spotifyService.skipToNext()
                            }
                        },
                        onPrevious = {
                            scope.launch {
                                spotifyService.skipToPrevious()
                            }
                        }
                    )
                }
                
                // BPM Dashboard Section (simplified without music integration)
                item {
                    BpmDashboardSection(
                        currentCadence = currentCadence,
                        bpmRecommendation = bpmRecommendation,
                        bpmSuggestion = null, // Temporarily disabled
                        currentTrack = currentTrack
                    )
                }
                
                // Audio Ducking Settings (temporarily disabled)
                item {
                    AudioDuckingSettingsSection(
                        audioDuckingLevel = 30, // Default value
                        musicAwareCoaching = true, // Default value
                        coachingPriority = MusicCoachingIntegration.CoachingPriority.NORMAL, // Default value
                        onDuckingLevelChange = { level ->
                            // Temporarily disabled until MusicCoachingIntegration is updated
                            println("Audio ducking level changed to: $level")
                        },
                        onMusicAwareCoachingChange = { enabled ->
                            // Temporarily disabled until MusicCoachingIntegration is updated
                            println("Music-aware coaching changed to: $enabled")
                        }
                    )
                }
                
                // Playlist Management Section
                item {
                    PlaylistManagementSection(
                        onGeneratePlaylist = {
                            scope.launch {
                                // Generate workout playlist
                                // This would integrate with PlaylistRecommendationEngine
                            }
                        },
                        onViewPlaylists = {
                            // Navigate to playlist management
                        }
                    )
                }
            }
            
            // Google Fit Connection Section
            item {
                GoogleFitConnectionSection(
                    connected = connectedApps.any { it.id == "google_fit" },
                    connecting = connectingApp == "google_fit",
                    onConnect = {
                        // Handle Google Fit connection
                        connectingApp = "google_fit"
                    }
                )
            }
        }
        
        // Complete button
        Button(
            onClick = { onComplete(connectedApps) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = connectedApps.isNotEmpty()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun SpotifyConnectionSection(
    spotifyConnected: Boolean,
    connecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (spotifyConnected) Primary.copy(alpha = 0.1f) else CardBackground,
        borderColor = if (spotifyConnected) Primary else CardBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SpotifyIcon(
                    tint = if (spotifyConnected) Primary else OnSurface,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spotify",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (spotifyConnected) Primary else OnSurface
                    )
                    Text(
                        text = if (spotifyConnected) "Connected" else "Connect your Spotify account",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (spotifyConnected) Primary else Neutral400
                    )
                }
                if (spotifyConnected) {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.Default.Disconnect,
                            contentDescription = "Disconnect",
                            tint = Neutral500
                        )
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        enabled = !connecting
                    ) {
                        if (connecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Connect")
                        }
                    }
                }
            }
            
            if (spotifyConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "• Access your playlists and music library\n• AI-powered workout playlist generation\n• BPM matching with your running cadence\n• Smart audio ducking during coaching",
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral400
                )
            }
        }
    }
}

@Composable
fun MusicControlsSection(
    currentTrack: SpotifyService.Track?,
    playbackState: SpotifyService.PlaybackState?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CardBackground
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Music Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current track info
            currentTrack?.let { track ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Track image placeholder
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Neutral500)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral400
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Playback controls
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = OnSurface
                    )
                }
                
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Primary)
                ) {
                    Icon(
                        imageVector = if (playbackState?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState?.isPlaying == true) "Pause" else "Play",
                        tint = Color.White
                    )
                }
                
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = OnSurface
                    )
                }
            }
        }
    }
}

@Composable
fun BpmDashboardSection(
    currentCadence: BPMAnalysisEngine.CadenceData?,
    bpmRecommendation: BPMAnalysisEngine.BpmRecommendation?,
    bpmSuggestion: MusicCoachingIntegration.BpmMatchingSuggestion?,
    currentTrack: SpotifyService.Track?
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CardBackground
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "BPM Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Cadence display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BpmMetricCard(
                    title = "Your Cadence",
                    value = currentCadence?.cadence?.toInt()?.toString() ?: "--",
                    unit = "spm",
                    color = Primary
                )
                
                BpmMetricCard(
                    title = "Music BPM",
                    value = bpmSuggestion?.currentTrackBpm?.toString() ?: "--",
                    unit = "BPM",
                    color = if (bpmSuggestion?.isGoodMatch == true) Primary else AppColors.Warning
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // BPM matching status
            bpmSuggestion?.let { suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (suggestion.isGoodMatch) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (suggestion.isGoodMatch) Primary else AppColors.Warning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = suggestion.suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (suggestion.isGoodMatch) Primary else AppColors.Warning
                    )
                }
            }
            
            // BPM difference
            bpmSuggestion?.let { suggestion ->
                if (suggestion.bpmDifference > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "BPM difference: ${suggestion.bpmDifference}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral400
                    )
                }
            }
        }
    }
}

@Composable
fun BpmMetricCard(
    title: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Neutral400
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = Neutral400
        )
    }
}

@Composable
fun AudioDuckingSettingsSection(
    audioDuckingLevel: Int,
    musicAwareCoaching: Boolean,
    coachingPriority: MusicCoachingIntegration.CoachingPriority,
    onDuckingLevelChange: (Int) -> Unit,
    onMusicAwareCoachingChange: (Boolean) -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CardBackground
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Audio Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Music-aware coaching toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Music-Aware Coaching",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                    Text(
                        text = "Time coaching messages with music structure",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral400
                    )
                }
                Switch(
                    checked = musicAwareCoaching,
                    onCheckedChange = onMusicAwareCoachingChange
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Audio ducking level
            Column {
                Text(
                    text = "Audio Ducking Level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface
                )
                Text(
                    text = "Reduce music volume during coaching: $audioDuckingLevel%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral400
                )
                Slider(
                    value = audioDuckingLevel.toFloat(),
                    onValueChange = { onDuckingLevelChange(it.toInt()) },
                    valueRange = 0f..50f,
                    steps = 9
                )
            }
            
            // Coaching priority indicator
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (coachingPriority) {
                        MusicCoachingIntegration.CoachingPriority.URGENT -> Icons.Default.PriorityHigh
                        MusicCoachingIntegration.CoachingPriority.HIGH -> Icons.Default.Warning
                        MusicCoachingIntegration.CoachingPriority.NORMAL -> Icons.Default.Info
                        MusicCoachingIntegration.CoachingPriority.LOW -> Icons.Default.LowPriority
                    },
                    contentDescription = null,
                    tint = when (coachingPriority) {
                        MusicCoachingIntegration.CoachingPriority.URGENT -> AppColors.Error
                        MusicCoachingIntegration.CoachingPriority.HIGH -> AppColors.Warning
                        MusicCoachingIntegration.CoachingPriority.NORMAL -> Primary
                        MusicCoachingIntegration.CoachingPriority.LOW -> Neutral400
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Coaching Priority: ${coachingPriority.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral400
                )
            }
        }
    }
}

@Composable
fun PlaylistManagementSection(
    onGeneratePlaylist: () -> Unit,
    onViewPlaylists: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CardBackground
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Playlist Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onGeneratePlaylist,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Playlist")
                }
                
                OutlinedButton(
                    onClick = onViewPlaylists,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Playlists")
                }
            }
        }
    }
}

@Composable
fun GoogleFitConnectionSection(
    connected: Boolean,
    connecting: Boolean,
    onConnect: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (connected) Primary.copy(alpha = 0.1f) else CardBackground,
        borderColor = if (connected) Primary else CardBorder
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            GoogleFitIcon(
                tint = if (connected) Primary else OnSurface,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Google Fit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (connected) Primary else OnSurface
                )
                Text(
                    text = if (connected) "Connected" else "Sync your fitness data",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connected) Primary else Neutral400
                )
            }
            if (!connected) {
                Button(
                    onClick = onConnect,
                    enabled = !connecting
                ) {
                    if (connecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Connect")
                    }
                }
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardBackground,
    borderColor: Color = CardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = CardDefaults.cardBorder(borderColor = borderColor),
        onClick = onClick ?: {}
    ) {
        content()
    }
}

@Composable
fun SpotifyIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Default.MusicNote,
        contentDescription = "Spotify",
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun GoogleFitIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Default.FitnessCenter,
        contentDescription = "Google Fit",
        tint = tint,
        modifier = modifier
    )
}

data class ConnectedApp(
    val id: String,
    val name: String,
    val type: AppType,
    val isConnected: Boolean = false
)

enum class AppType {
    SPOTIFY,
    GOOGLE_FIT
}
